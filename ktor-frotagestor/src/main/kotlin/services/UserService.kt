package com.frotagestor.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.UsersTable
import com.frotagestor.interfaces.LoginResponse
import com.frotagestor.interfaces.Message
import com.frotagestor.interfaces.PaginatedResponse
import com.frotagestor.plugins.JwtConfig
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.User
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateLogin
import com.frotagestor.validations.validatePartialUser
import com.frotagestor.validations.validateUser
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class UserService {
    suspend fun login(req: String): ServiceResponse<LoginResponse> {
        val loginReq = validateLogin(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = LoginResponse.Error(msg)
            )
        }

        val resDb = DatabaseFactory.dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq loginReq.username }
                .singleOrNull()
        }

        if (resDb == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = LoginResponse.Error("Usuário não encontrado")
            )
        }

        val storedHash = resDb[UsersTable.password]
        val verified = BCrypt.verifyer().verify(loginReq.password.toCharArray(), storedHash)

        if (!verified.verified) {
            return ServiceResponse(
                status = HttpStatusCode.Unauthorized,
                data = LoginResponse.Error("Senha inválida")
            )
        }

        val user = User(
            id = resDb[UsersTable.id],
            username = resDb[UsersTable.username],
            role = resDb[UsersTable.role]
        )

        val token = JwtConfig.generateToken(user.id.toString(), user.role, user.username)

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = LoginResponse.Success(token)
        )
    }


    suspend fun createUser(req: String): ServiceResponse<Message> {
        val newUser = validateUser(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingUser = DatabaseFactory.dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq newUser.username }
                .singleOrNull()
        }

        if (existingUser != null) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Usuário já registrado!")
            )
        }

        val hashedPassword = BCrypt.withDefaults()
            .hashToString(12, newUser.password!!.toCharArray())

        DatabaseFactory.dbQuery {
            UsersTable.insert {
                it[username] = newUser.username
                it[password] = hashedPassword
                it[role] = newUser.role
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Usuário criado com sucesso")
        )
    }

    suspend fun updateUser(id: Int, req: String): ServiceResponse<Message> {
        val updatedUser = validatePartialUser(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingUser = DatabaseFactory.dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
        }

        if (existingUser == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Usuário não encontrado!")
            )
        }

        val hashedPassword = updatedUser.password?.let {
            BCrypt.withDefaults().hashToString(12, it.toCharArray())
        }

        DatabaseFactory.dbQuery {
            UsersTable.update({ UsersTable.id eq id }) {
                updatedUser.username.takeIf { !it.isNullOrBlank() }?.let { u -> it[username] = u }
                hashedPassword?.let { pwd -> it[password] = pwd }
                updatedUser.role.takeIf { !it.isNullOrBlank() }?.let { r -> it[role] = r }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Usuário atualizado com sucesso")
        )
    }

    suspend fun deleteUser(id: Int): ServiceResponse<Message> {
        val existingUser = DatabaseFactory.dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
        }

        if (existingUser == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Usuário não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            UsersTable.deleteWhere { UsersTable.id eq id }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Usuário deletado com sucesso")
        )
    }

    suspend fun getAllUsers(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = UsersTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        usernameFilter: String? = null,
        roleFilter: String? = null
    ): ServiceResponse<PaginatedResponse<User>> {
        return DatabaseFactory.dbQuery {
            val query = UsersTable
                .selectAll()
                .apply {
                    if (idFilter != null) {
                        andWhere { UsersTable.id eq idFilter }
                    }
                    if (!usernameFilter.isNullOrBlank()) {
                        andWhere { UsersTable.username like "%$usernameFilter%" }
                    }
                    if (!roleFilter.isNullOrBlank()) {
                        andWhere { UsersTable.role eq roleFilter }
                    }
                }

            val total = query.count()

            val results = query
                .orderBy(sortBy to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    User(
                        id = it[UsersTable.id],
                        username = it[UsersTable.username],
                        role = it[UsersTable.role]
                    )
                }

            ServiceResponse(
                status = HttpStatusCode.OK,
                data = PaginatedResponse(
                    data = results,
                    total = total.toInt(),
                    page = page,
                    limit = limit,
                    totalPages = if (total == 0L) 0 else ((total + limit - 1) / limit).toInt()
                )
            )
        }
    }

    suspend fun findUserById(id: Int): ServiceResponse<Any> {
        val user = DatabaseFactory.dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?.let {
                    User(
                        id = it[UsersTable.id],
                        username = it[UsersTable.username],
                        role = it[UsersTable.role]
                    )
                }
        }

        return if (user == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = mapOf("message" to "Usuário não encontrado")
            )
        } else {
            ServiceResponse(
                status = HttpStatusCode.OK,
                data = user
            )
        }
    }
}
