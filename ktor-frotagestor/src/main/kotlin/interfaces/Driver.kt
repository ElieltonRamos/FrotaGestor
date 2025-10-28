package com.frotagestor.interfaces

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Driver(
    val id: Int? = null,
    val name: String,
    val cpf: String,
    val cnh: String,
    val cnhCategory: String? = null,
    val cnhExpiration: LocalDate? = null,
    val phone: String? = null,
    val email: String? = null,
    val status: DriverStatus = DriverStatus.ATIVO,
    val deletedAt: LocalDateTime? = null
)

@Serializable
data class PartialDriver(
    val name: String? = null,
    val cpf: String? = null,
    val cnh: String? = null,
    val cnhCategory: String? = null,
    val cnhExpiration: LocalDate? = null,
    val phone: String? = null,
    val email: String? = null,
    val status: DriverStatus? = null
)

@Serializable
enum class DriverStatus {
    ATIVO,
    INATIVO
}

@Serializable
data class DriverIndicators(
    val total: Int,
    val withExpiredLicense: Int,
    val withExpiringLicense: Int,
    val mostCommonCategory: String? = null,
    val lastDriver: LastDriver
) {
    @Serializable
    data class LastDriver(
        val name: String,
        val cpf: String,
    )
}

@Serializable
data class DriverReport(
    val distributions: Distributions,
    val driversStats: List<DriverStats>
) {
    @Serializable
    data class Distributions(
        val totalDrivers: Int,
        val cnhExpiringSoon: Int,
        val cnhExpired: Int,
        val byCategory: List<ByCategory>
    ) {
        @Serializable
        data class ByCategory(
            val category: String,
            val count: Int
        )
    }

    @Serializable
    data class DriverStats(
        val driverName: String,
        val driverId: Int,
        val totalTrips: Int,
        val totalDistance: Double,
        val totalCost: Double,
        val averageFuelConsumption: Double? = null,
        val lastTripDate: String? = null
    )
}