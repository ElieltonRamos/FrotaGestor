package com.frotagestor.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

object JwtConfig {
    private val secret: String = System.getenv("JWT_SECRET") ?: "dev-secret"
    private val issuer: String = System.getenv("JWT_ISSUER") ?: "com.frotagestor.app"

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: String, role: String, username: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withClaim("username", username)
            .sign(algorithm)
    }
}
