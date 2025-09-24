package com.frotagestor.validations

import com.frotagestor.interfaces.Vehicle
import com.frotagestor.interfaces.PartialVehicle
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun validateVehicle(rawBody: String): ValidationResult<Vehicle> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val vehicle: Vehicle = try {
        Json.decodeFromString<Vehicle>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (vehicle.plate.isBlank()) missingFields.add("plate")
    if (vehicle.model.isBlank()) missingFields.add("model")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(vehicle)
    }
}

fun validatePartialVehicle(rawBody: String): ValidationResult<PartialVehicle> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val vehicle = try {
        Json.decodeFromString<PartialVehicle>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    if (
        vehicle.plate?.isBlank() == true &&
        vehicle.model?.isBlank() == true &&
        vehicle.brand.isNullOrBlank() &&
        vehicle.year == null &&
        vehicle.status == null
    ) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    return ValidationResult.Success(vehicle)
}
