package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
enum class TripStatus {
    Planned,
    InProgress,
    Completed,
    Cancelled
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
    val status: TripStatus = TripStatus.Planned
)