package com.frotagestor.interfaces
import kotlinx.datetime.LocalDateTime

data class GpsDevice(
    val id: Int,                  // ID do dispositivo
    val vehicleId: Int,           // Veículo vinculado
    val imei: String,             // Identificador do GPS
    val latitude: Double,         // Última latitude
    val longitude: Double,        // Última longitude
    val dateTime: LocalDateTime,  // Momento da leitura
    val speed: Double = 0.0,      // Velocidade (opcional)
    val heading: Double = 0.0,    // Direção (opcional)
    val iconMapUrl: String? = null,// Ícone para o mapa
    val title: String? = null,     // Modelo + placa
    val ignition: Boolean = false  // Ignição ligada/desligada
)

data class PartialGpsDevice(
    val vehicleId: Int? = null,
    val imei: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dateTime: LocalDateTime? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val iconMapUrl: String? = null,
    val title: String? = null,
    val ignition: Boolean? = null
)