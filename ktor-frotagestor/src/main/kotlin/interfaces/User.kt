package com.redenorte.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

data class User(
    val id: Int? = null,
    val username: String,
    val password: String,
    val role: String
)