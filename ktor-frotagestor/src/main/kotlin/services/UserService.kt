package com.frotagestor.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.UsersTable
import com.frotagestor.interfaces.LoginResponse
import com.frotagestor.interfaces.Message
import com.frotagestor.plugins.JwtConfig
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.User
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateLogin
import com.frotagestor.validations.validateUser
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

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

        val token = JwtConfig.generateToken(user.id.toString(), user.role)

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
}
