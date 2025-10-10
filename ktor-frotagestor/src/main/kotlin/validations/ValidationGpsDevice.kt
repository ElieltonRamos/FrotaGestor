package com.frotagestor.validations

import com.frotagestor.interfaces.GpsDevice
import com.frotagestor.interfaces.PartialGpsDevice
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun validateGpsDevice(rawBody: String): ValidationResult<GpsDevice> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val gpsDevice: GpsDevice = try {
        Json.decodeFromString<GpsDevice>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (gpsDevice.vehicleId <= 0) missingFields.add("vehicleId")
    if (gpsDevice.imei.isBlank()) missingFields.add("imei")
    if (gpsDevice.latitude == null) missingFields.add("latitude")
    if (gpsDevice.longitude == null) missingFields.add("longitude")
    if (gpsDevice.dateTime == null) missingFields.add("dateTime")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(gpsDevice)
    }
}

fun validatePartialGpsDevice(rawBody: String): ValidationResult<PartialGpsDevice> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val gpsDevice = try {
        Json.decodeFromString<PartialGpsDevice>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    if (
        gpsDevice.imei.isNullOrBlank() &&
        gpsDevice.latitude == null &&
        gpsDevice.longitude == null &&
        gpsDevice.dateTime == null &&
        gpsDevice.speed == null &&
        gpsDevice.heading == null &&
        gpsDevice.iconMapUrl.isNullOrBlank() &&
        gpsDevice.title.isNullOrBlank() &&
        gpsDevice.ignition == null
    ) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    return ValidationResult.Success(gpsDevice)
}
