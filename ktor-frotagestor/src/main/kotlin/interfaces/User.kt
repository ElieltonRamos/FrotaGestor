package com.redenorte.interfaces

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
sealed class LoginResponse {
    data class Success(val token: String, val user: User) : LoginResponse()
    data class Error(val message: String) : LoginResponse()
}

data class User(
    val id: Int? = null,
    val username: String,
    val password: String,
    val role: String
)