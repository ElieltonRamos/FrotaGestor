package com.frotagestor.interfaces

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: Int? = null,
    val plate: String,
    val model: String,
    val brand: String? = null,
    val year: Int? = null,
    val status: VehicleStatus = VehicleStatus.ATIVO
)

@Serializable
enum class VehicleStatus {
    ATIVO,
    INATIVO,
    MANUTENCAO
}

@Serializable
data class PartialVehicle(
    val id: Int? = null,
    val plate: String? = null,
    val model: String? = null,
    val brand: String? = null,
    val year: Int? = null,
    val status: VehicleStatus = VehicleStatus.ATIVO
)

@Serializable
data class VehicleIndicators(
    val active: Int,
    val maintenance: Int,
    val lastVehicle: Vehicle? = null
)

@Serializable
data class VehicleReport(
    val distributions: Distributions,
    val usageStats: UsageStats
) {
    @Serializable
    data class Distributions(
        val byBrand: List<ByBrand>,
        val byYear: List<ByYear>,
        val byStatus: List<ByStatus>
    ) {
        @Serializable
        data class ByBrand(
            val brand: String,
            val count: Long
        )

        @Serializable
        data class ByYear(
            val year: Int,
            val count: Long
        )

        @Serializable
        data class ByStatus(
            val status: Status,
            val count: Long
        )

        @Serializable
        enum class Status {
            ATIVO,
            MANUTENCAO,
            INATIVO
        }
    }

    @Serializable
    data class UsageStats(
        val totalDistanceByVehicle: List<TotalDistanceByVehicle>,
        val fuelConsumptionByVehicle: List<FuelConsumptionByVehicle>
    ) {
        @Serializable
        data class TotalDistanceByVehicle(
            val plate: String,
            val totalKm: Int,
            val totalTrips: Long,
            val topDriver: TopDriver? = null,
            val fuelCost: Double,
            val maintenanceCost: Double,
            val totalCost: Double,
            val lastMaintenanceDate: String? = null,
            val isInUse: Boolean? = null
        ) {
            @Serializable
            data class TopDriver(
                val name: String?,
                val trips: Int
            )
        }

        @Serializable
        data class FuelConsumptionByVehicle(
            val plate: String,
            val litersPerKm: Double
        )
    }
}
