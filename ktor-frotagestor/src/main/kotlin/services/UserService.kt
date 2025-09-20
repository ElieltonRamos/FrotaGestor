package com.redenorte.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.redenorte.database.DatabaseFactory
import com.redenorte.database.models.UsersTable
import com.redenorte.interfaces.LoginResponse
import com.redenorte.interfaces.ServiceResponse
import com.redenorte.interfaces.User
import com.redenorte.plugins.JwtConfig
import io.ktor.http.HttpStatusCode
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
}

