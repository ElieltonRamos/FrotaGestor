package com.redenorte.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private const val secret = "super-secret-key"   // 👉 coloque isso em env/config
    private const val issuer = "com.redenorte"

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: String, role: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .sign(algorithm)
    }
}
