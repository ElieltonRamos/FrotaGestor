package com.frotagestor.validations

import com.frotagestor.interfaces.Subfleet
import com.frotagestor.interfaces.PartialSubfleet
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Valida os dados de uma nova subfrota
 */
fun validateSubfleet(rawBody: String): ValidationResult<Subfleet> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val subfleet: Subfleet = try {
        Json.decodeFromString<Subfleet>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    // Validar campos obrigatórios
    val missingFields = mutableListOf<String>()
    if (subfleet.name.isBlank()) missingFields.add("name")

    if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        return ValidationResult.Error(msg)
    }

    return ValidationResult.Success(subfleet)
}

/**
 * Valida os dados de atualização parcial de uma subfrota
 */
fun validatePartialSubfleet(rawBody: String): ValidationResult<PartialSubfleet> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val subfleet = try {
        Json.decodeFromString<PartialSubfleet>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    // Verificar se pelo menos um campo foi fornecido
    if (subfleet.name.isNullOrBlank() && subfleet.description == null) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    // Validar nome não vazio se fornecido
    if (subfleet.name?.isBlank() == true) {
        return ValidationResult.Error("O nome não pode estar vazio")
    }

    return ValidationResult.Success(subfleet)
}
