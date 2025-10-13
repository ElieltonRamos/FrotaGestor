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

@Serializable
data class TripIndicators(
    val totalTrips: Int,
    val planned: Int,
    val inProgress: Int,
    val completed: Int,
    val canceled: Int,
    val totalDistance: Double,
    val avgDistance: Double,
    val lastTrip: LastTrip?
)

@Serializable
data class LastTrip(
    val date: String,
    val driverName: String,
    val vehiclePlate: String
)

@Serializable
data class TripReport(
    val distributions: TripDistributions
)

@Serializable
data class TripDistributions(
    val byStatus: List<StatusDistribution>,
    val byVehicle: List<VehicleDistribution>,
    val byDriver: List<DriverDistribution>,
    val byDestination: List<DestinationDistribution>
)

@Serializable
data class StatusDistribution(
    val status: TripStatus,
    val count: Int
)

@Serializable
data class VehicleDistribution(
    val vehiclePlate: String,
    val count: Int,
    val totalCost: Double
)

@Serializable
data class DriverDistribution(
    val driverName: String,
    val count: Int,
    val totalCost: Double
)

@Serializable
data class DestinationDistribution(
    val destination: String,
    val totalTrips: Int,
    val totalCost: Double
)
