package com.frotagestor.interfaces

import java.math.BigDecimal
import java.time.LocalDateTime

enum class TripStatus {
    Planned,
    InProgress,
    Completed,
    Cancelled
}

data class Trip(
    val id: Int? = null,
    val vehicleId: Int,
    val driverId: Int,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val distanceKm: BigDecimal? = null,
    val status: TripStatus = TripStatus.Planned
)