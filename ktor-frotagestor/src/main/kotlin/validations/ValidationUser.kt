package com.frotagestor.validations

import com.frotagestor.interfaces.User
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed class ValidationResult<out T> {
    data class Success<T>(val data: T) : ValidationResult<T>()
    data class Error(val message: String) : ValidationResult<Nothing>()
}

inline fun <T, R> ValidationResult<T>.getOrReturn(
    onError: (String) -> R
): T {
    return when (this) {
        is ValidationResult.Error -> return onError(this.message) as T
        is ValidationResult.Success -> this.data
    }
}


fun validateUser(rawBody: String): ValidationResult<User> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val user: User = try {
        Json.decodeFromString<User>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (user.username.isBlank()) missingFields.add("username")
    if (user.password.isNullOrBlank()) missingFields.add("password")
    if (user.role.isBlank()) missingFields.add("role")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(user)
    }
}
