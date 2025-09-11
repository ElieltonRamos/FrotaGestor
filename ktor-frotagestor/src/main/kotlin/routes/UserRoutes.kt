package com.redenorte.routes

import com.redenorte.models.LoginRequest
import com.redenorte.models.LoginResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.*

fun Route.userRoutes() {
    post("/login") {
        println("Login endpoint chamado")
        try {
            val request = call.receive<LoginRequest>()
            println("Request recebido: $request")

            // Login de teste
            if (request.username == "admin" && request.password == "1234") {
                // Gerar token JWT
                val token = JWT.create()
                    .withSubject("Authentication")
                    .withClaim("username", request.username)
                    .withExpiresAt(Date(System.currentTimeMillis() + 60000)) // 1 minuto
                    .sign(Algorithm.HMAC256("secret"))

                call.respond(HttpStatusCode.OK, LoginResponse(token = token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Usuário ou senha inválidos"))
            }
        } catch (e: Exception) {
            println("Erro ao processar requisição: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Requisição inválida: ${e.message}"))
        }
    }
}
