package com.frotagestor.validations

import com.frotagestor.interfaces.Trip
import com.frotagestor.interfaces.PartialTrip
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

fun validateTrip(rawBody: String): ValidationResult<Trip> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val trip: Trip = try {
        Json.decodeFromString<Trip>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (trip.vehicleId <= 0) missingFields.add("vehicleId")
    trip.driverId?.let { if (it <= 0) missingFields.add("driverId") }
    if (trip.startTime == null) missingFields.add("startTime")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(trip)
    }
}

fun validatePartialTrip(rawBody: String): ValidationResult<PartialTrip> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val trip = try {
        lenientJson.decodeFromString<PartialTrip>(rawBody)
    } catch (e: SerializationException) {
        println("❌ ERRO SERIALIZAÇÃO:")
        println("Mensagem: ${e.message}")
        println("Causa: ${e.cause}")
        return ValidationResult.Error("JSON inválido: Verifique a Estrutura dos dados")
    } catch (e: Exception) {
        println("❌ ERRO GERAL: ${e.message}")
        return ValidationResult.Error("Erro de parsing: Servidor")
    }
    if (
        trip.vehicleId == null &&
        trip.driverId == null &&
        trip.startLocation.isNullOrBlank() &&
        trip.endLocation.isNullOrBlank() &&
        trip.startTime == null &&
        trip.endTime == null &&
        trip.distanceKm == null &&
        trip.status == null
    ) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    return ValidationResult.Success(trip)
}

