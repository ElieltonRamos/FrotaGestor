package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Subfleet(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val vehicleCount: Int = 0,
    val activeVehicleCount: Int = 0
)

@Serializable
data class PartialSubfleet(
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class SubfleetReport(
    val subfleetId: Int,
    val totalVehicles: Int,
    val activeVehicles: Int,
    val maintenanceVehicles: Int,
    val totalTrips: Int,
    val totalDistanceKm: Double,
    val totalExpenses: Double
)

@Serializable
data class SubfleetIndicators(
    val totalActive: Int,
    val totalVehicles: Int,
    val lastSubfleet: Subfleet? = null
)