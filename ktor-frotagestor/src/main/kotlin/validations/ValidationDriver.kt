package com.frotagestor.validations

import com.frotagestor.interfaces.Driver
import com.frotagestor.interfaces.PartialDriver
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun validateDriver(rawBody: String): ValidationResult<Driver> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val driver: Driver = try {
        Json.decodeFromString<Driver>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (driver.name.isBlank()) missingFields.add("name")
    if (driver.cpf.isBlank()) missingFields.add("cpf")
    if (driver.cnh.isBlank()) missingFields.add("cnh")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(driver)
    }
}

fun validatePartialDriver(rawBody: String): ValidationResult<PartialDriver> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val driver = try {
        Json.decodeFromString<PartialDriver>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    if (
        driver.name?.isBlank() == true &&
        driver.cpf?.isBlank() == true &&
        driver.cnh?.isBlank() == true &&
        driver.cnhCategory.isNullOrBlank() &&
        driver.cnhExpiration == null &&
        driver.phone.isNullOrBlank() &&
        driver.email.isNullOrBlank()
    ) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    return ValidationResult.Success(driver)
}
