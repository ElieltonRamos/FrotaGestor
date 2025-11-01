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
        return ValidationResult.Error("JSON inválido: ${e.message}")
    }

    // Validação do IMEI (obrigatório)
    if (gpsDevice.imei.isNullOrBlank()) {
        return ValidationResult.Error("O campo IMEI é obrigatório")
    }

    // Validação do iconMapUrl (obrigatório)
    if (gpsDevice.iconMapUrl.isNullOrBlank()) {
        return ValidationResult.Error("O campo iconMapUrl é obrigatório")
    }

    // Validação de coordenadas (se fornecidas)
    gpsDevice.latitude?.let { lat ->
        if (lat < -90.0 || lat > 90.0) {
            return ValidationResult.Error("Latitude inválida. Deve estar entre -90 e 90")
        }
    }

    gpsDevice.longitude?.let { lon ->
        if (lon < -180.0 || lon > 180.0) {
            return ValidationResult.Error("Longitude inválida. Deve estar entre -180 e 180")
        }
    }

    // Validação de velocidade (se fornecida)
    gpsDevice.speed?.let { speed ->
        if (speed < 0.0) {
            return ValidationResult.Error("Velocidade não pode ser negativa")
        }
    }

    // Validação de heading/direção (se fornecida)
    gpsDevice.heading?.let { heading ->
        if (heading < 0.0 || heading > 360.0) {
            return ValidationResult.Error("Direção inválida. Deve estar entre 0 e 360")
        }
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
        return ValidationResult.Error("JSON inválido: ${e.message}")
    }

    // Verifica se pelo menos um campo foi fornecido para atualização
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
    ) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    // Coordenadas (se fornecidas)
    gpsDevice.latitude?.let { lat ->
        if (lat < -90.0 || lat > 90.0) {
            return ValidationResult.Error("Latitude inválida. Deve estar entre -90 e 90")
        }
    }

    gpsDevice.longitude?.let { lon ->
        if (lon < -180.0 || lon > 180.0) {
            return ValidationResult.Error("Longitude inválida. Deve estar entre -180 e 180")
        }
    }

    // Velocidade (se fornecida)
    gpsDevice.speed?.let { speed ->
        if (speed < 0.0) {
            return ValidationResult.Error("Velocidade não pode ser negativa")
        }
    }

    // Heading/direção (se fornecida)
    gpsDevice.heading?.let { heading ->
        if (heading < 0.0 || heading > 360.0) {
            return ValidationResult.Error("Direção inválida. Deve estar entre 0 e 360")
        }
    }

    return ValidationResult.Success(gpsDevice)
}