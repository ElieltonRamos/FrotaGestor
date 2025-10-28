package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class GpsDevice(
    val id: Int,                           // ID do dispositivo
    val vehicleId: Int? = null,            // Veículo vinculado (NULLABLE - permite dispositivos sem veículos)
    val imei: String,                      // Identificador do GPS
    val latitude: Double = 0.0,            // Última latitude (padrão 0 para dispositivos novos)
    val longitude: Double = 0.0,           // Última longitude (padrão 0 para dispositivos novos)
    val dateTime: LocalDateTime? = null,   // Momento da leitura (nullable para dispositivos novos)
    val speed: Double = 0.0,               // Velocidade (opcional)
    val heading: Double = 0.0,             // Direção (opcional)
    val iconMapUrl: String? = null,        // Ícone para o mapa
    val title: String? = null,             // Modelo + placa
    val ignition: Boolean = false          // Ignição ligada/desligada
)

@Serializable
data class PartialGpsDevice(
    val vehicleId: Int? = null,            // Permite atualizar ou remover (null) o veículo vinculado
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