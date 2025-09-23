package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class FuelLog(
    val id: Int? = null,
    val vehicleId: Int,
    val driverId: Int? = null,
    val date: LocalDateTime,
    val liters: Double,
    val cost: Double,
    val station: String? = null
)