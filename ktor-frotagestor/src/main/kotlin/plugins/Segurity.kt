package com.redenorte.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            realm = "ktor sample app"
            verifier(
                JWT
                    .require(Algorithm.HMAC256("secret")) // chave secreta
                    .withIssuer("myapp")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") JWTPrincipal(credential.payload)
                else null
            }
        }
    }
}
