package com.frotagestor.validations

import com.frotagestor.interfaces.CommandRequest
import com.frotagestor.interfaces.PartialGpsDevice
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun validateGpsDevice(rawBody: String): ValidationResult<PartialGpsDevice> {
    if (rawBody.isBlank())
        return ValidationResult.Error("Body da requisição está vazio")

    val gpsDevice = try {
        json.decodeFromString<PartialGpsDevice>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido: ${e.message}")
    }

    if (gpsDevice.imei.isNullOrBlank())
        return ValidationResult.Error("O campo IMEI é obrigatório")

    if (gpsDevice.iconMapUrl.isNullOrBlank())
        return ValidationResult.Error("O campo iconMapUrl é obrigatório")

    gpsDevice.latitude?.let {
        if (it < -90.0 || it > 90.0)
            return ValidationResult.Error("Latitude inválida. Deve estar entre -90 e 90")
    }

    gpsDevice.longitude?.let {
        if (it < -180.0 || it > 180.0)
            return ValidationResult.Error("Longitude inválida. Deve estar entre -180 e 180")
    }

    gpsDevice.speed?.let {
        if (it < 0.0)
            return ValidationResult.Error("Velocidade não pode ser negativa")
    }

    gpsDevice.heading?.let {
        if (it < 0.0 || it > 360.0)
            return ValidationResult.Error("Direção inválida. Deve estar entre 0 e 360")
    }

    return ValidationResult.Success(gpsDevice)
}

fun validatePartialGpsDevice(rawBody: String): ValidationResult<PartialGpsDevice> {
    if (rawBody.isBlank())
        return ValidationResult.Error("Body da requisição está vazio")

    val gpsDevice = try {
        json.decodeFromString<PartialGpsDevice>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido: ${e.message}")
    }

    if (
        gpsDevice.imei.isNullOrBlank() &&
        gpsDevice.vehicleId == null &&
        gpsDevice.latitude == null &&
        gpsDevice.longitude == null &&
        gpsDevice.dateTime == null &&
        gpsDevice.speed == null &&
        gpsDevice.heading == null &&
        gpsDevice.iconMapUrl.isNullOrBlank() &&
        gpsDevice.title.isNullOrBlank() &&
        gpsDevice.ignition == null
    ) return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")

    gpsDevice.latitude?.let {
        if (it < -90.0 || it > 90.0)
            return ValidationResult.Error("Latitude inválida. Deve estar entre -90 e 90")
    }

    gpsDevice.longitude?.let {
        if (it < -180.0 || it > 180.0)
            return ValidationResult.Error("Longitude inválida. Deve estar entre -180 e 180")
    }

    gpsDevice.speed?.let {
        if (it < 0.0)
            return ValidationResult.Error("Velocidade não pode ser negativa")
    }

    gpsDevice.heading?.let {
        if (it < 0.0 || it > 360.0)
            return ValidationResult.Error("Direção inválida. Deve estar entre 0 e 360")
    }

    return ValidationResult.Success(gpsDevice)
}

fun validateCommandRequest(rawBody: String): ValidationResult<CommandRequest> {
    if (rawBody.isBlank())
        return ValidationResult.Error("Body da requisição está vazio")

    val request = try {
        json.decodeFromString<CommandRequest>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido: ${e.message}")
    }

    if (request.commandType.isBlank())
        return ValidationResult.Error("O campo commandType é obrigatório e não pode estar vazio")

    if (request.deviceId.isBlank())
        return ValidationResult.Error("O campo deviceId é obrigatório e não pode estar vazio")

    request.parameters.forEach { (key, value) ->
        if (key.isBlank())
            return ValidationResult.Error("Parâmetro com chave vazia não é permitido")
        if (value.isBlank())
            return ValidationResult.Error("Parâmetro com valor vazio para a chave '$key' não é permitido")
    }

    return ValidationResult.Success(request)
}