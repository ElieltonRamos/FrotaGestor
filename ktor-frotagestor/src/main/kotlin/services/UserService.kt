package com.redenorte.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.redenorte.database.models.UsersTable
import com.redenorte.interfaces.ServiceResponse
import com.redenorte.interfaces.LoginResponse
import com.redenorte.interfaces.User
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserService {
    fun login(username: String, password: String): ServiceResponse<LoginResponse> {
        // busca usuário no banco
        val userRow = transaction {
            UsersTable
                .selectAll().where { UsersTable.username eq username }
                .singleOrNull()
        }

        if (userRow == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = LoginResponse.Error("Usuário não encontrado")
            )
        }

        val storedHash = userRow[UsersTable.password]
        val role = userRow[UsersTable.role]
        val user = User(
            id = userRow[UsersTable.id],
            username = userRow[UsersTable.username],
            role = role,
            password = storedHash
        )

        // valida senha
        val result = BCrypt.verifyer().verify(password.toCharArray(), storedHash)
        if (!result.verified) {
            return ServiceResponse(
                status = HttpStatusCode.Unauthorized,
                data = LoginResponse.Error("Senha inválida")
            )
        }

        // gera token JWT (aqui seria JwtConfig.generateToken)
        val token = "fake-jwt-token-${user.username}"

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = LoginResponse.Success(token, user)
        )
    }
}
