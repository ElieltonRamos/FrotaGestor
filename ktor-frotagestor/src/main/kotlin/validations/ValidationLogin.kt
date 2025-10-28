package com.frotagestor.validations

import com.frotagestor.interfaces.LoginRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun validateLogin(rawBody: String): ValidationResult<LoginRequest> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Corpo da requisição está vazio")
    }

    val login: LoginRequest = try {
        Json.decodeFromString<LoginRequest>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (login.username.isBlank()) missingFields.add("username")
    if (login.password.isBlank()) missingFields.add("password")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(login)
    }
}
