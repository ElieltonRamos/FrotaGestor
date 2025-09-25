package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class TripStatus {
    PLANEJADA,
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA
}


@Serializable
data class Trip(
    val id: Int? = null,
    val vehicleId: Int,
    val driverId: Int,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val distanceKm: Double? = null,
    val status: TripStatus = TripStatus.PLANEJADA,
    val driverName: String? = null,
    val vehiclePlate: String? = null
)

@Serializable
data class PartialTrip(
    val vehicleId: Int? = null,
    val driverId: Int? = null,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val distanceKm: Double? = null,
    val status: TripStatus? = null
)