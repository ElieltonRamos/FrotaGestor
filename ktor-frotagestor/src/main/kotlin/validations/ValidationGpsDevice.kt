package com.frotagestor.validations

import com.frotagestor.interfaces.PartialGpsDevice
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun validateGpsDevice(rawBody: String): ValidationResult<PartialGpsDevice> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val gpsDevice: PartialGpsDevice = try {
        Json.decodeFromString<PartialGpsDevice>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    if (gpsDevice.imei.isNullOrBlank()) {
        return ValidationResult.Error("O campo IMEI é obrigatório")
    }

    return ValidationResult.Success(gpsDevice)
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
