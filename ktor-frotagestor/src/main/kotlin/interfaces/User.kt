package com.frotagestor.interfaces

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class LoginRequest(val username: String, val password: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("status")
sealed class LoginResponse {
    @Serializable
    @SerialName("success")
    data class Success(val token: String) : LoginResponse()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : LoginResponse()
}


@Serializable
data class User(
    val id: Int? = null,
    val username: String,
    val password: String? = null,
    val role: String
)
