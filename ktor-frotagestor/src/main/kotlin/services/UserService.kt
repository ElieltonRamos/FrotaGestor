package com.frotagestor.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.UsersTable
import com.frotagestor.interfaces.LoginResponse
import com.frotagestor.plugins.JwtConfig
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.User
import com.frotagestor.validations.ValidationResult
import com.frotagestor.validations.validateUser
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class UserService {
    suspend fun login(username: String, password: String): ServiceResponse<LoginResponse> {
        val result = DatabaseFactory.dbQuery {
            UsersTable
                .selectAll().where { UsersTable.username eq username }
                .singleOrNull()
                ?.let {
                    val user = User(
                        id = it[UsersTable.id],
                        username = it[UsersTable.username],
                        role = it[UsersTable.role]
                    )
                    val storedHash = it[UsersTable.password]
                    user to storedHash
                }
        }

        if (result == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = LoginResponse.Error("Usuário não encontrado")
            )
        }

        val (user, storedHash) = result
        val verified = BCrypt.verifyer().verify(password.toCharArray(), storedHash)

        if (!verified.verified) {
            return ServiceResponse(
                status = HttpStatusCode.Unauthorized,
                data = LoginResponse.Error("Senha inválida")
            )
        }

        val token = JwtConfig.generateToken(user.id.toString(), user.role)

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = LoginResponse.Success(token)
        )
    }

    suspend fun createUser(req: String): ServiceResponse<String> {
        return when (val result = validateUser(req)) {
            is ValidationResult.Error -> {
                ServiceResponse(
                    status = HttpStatusCode.BadRequest,
                    data = result.message
                )
            }

            is ValidationResult.Success -> {
                val newUser = result.data
                val existingUser = DatabaseFactory.dbQuery {
                    UsersTable
                        .selectAll()
                        .where { UsersTable.username eq newUser.username }
                        .singleOrNull()
                }

                if (existingUser != null) {
                    return ServiceResponse(
                        status = HttpStatusCode.Conflict,
                        data = "Usuário já registrado!"
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

                ServiceResponse(
                    status = HttpStatusCode.Created,
                    data = "Usuário criado com sucesso"
                )
            }
        }
    }

}
