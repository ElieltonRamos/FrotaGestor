package com.redenorte.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            realm = "Access to 'api'"
            verifier(
                JWT
                    .require(Algorithm.HMAC256("super-secret-key")) // chave secreta
                    .withIssuer("com.redenorte")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString().isNotEmpty()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
