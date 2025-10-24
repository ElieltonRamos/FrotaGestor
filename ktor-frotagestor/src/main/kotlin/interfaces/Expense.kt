package com.frotagestor.interfaces

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class Expense(
    val id: Int? = null,
    val vehicleId: Int? = null,
    val driverId: Int? = null,
    val tripId: Int? = null,
    val date: LocalDate,
    val type: String,
    val description: String? = null,
    val amount: Double,
    val liters: Double? = null,
    val pricePerLiter: Double? = null,
    val odometer: Int? = null,
    val driverName: String? = null,
    val vehiclePlate: String? = null
)

@Serializable
data class PartialExpense(
    val id: Int? = null,
    val vehicleId: Int? = null,
    val driverId: Int? = null,
    val tripId: Int? = null,
    val date: String? = null,
    val type: String? = null,
    val description: String? = null,
    val amount: Double? = null,
    val liters: Double? = null,
    val pricePerLiter: Double? = null,
    val odometer: Int? = null
)

@Serializable
data class RefuelingIndicators(
    val totalAmount: Double,
    val totalLiters: Double,
    val avgPricePerLiter: Double,
    val topDriver: TopDriver? = null,
    val topVehicleByAmount: TopVehicleAmount? = null,
    val topVehicleByLiters: TopVehicleLiters? = null,
    val lastRefueling: LastRefueling? = null
) {
    @Serializable
    data class TopDriver(val name: String, val count: Int)
    @Serializable
    data class TopVehicleAmount(val plate: String, val amount: Double)
    @Serializable
    data class TopVehicleLiters(val plate: String, val liters: Double)
    @Serializable
    data class LastRefueling(val date: String, val plate: String)
}

@Serializable
data class MaintenanceIndicators(
    val totalAmount: Double,
    val totalCount: Int,
    val mostCommonType: String,
    val topVehicleByAmount: TopVehicleAmount,
    val lastMaintenance: LastMaintenance
) {
    @Serializable
    data class TopVehicleAmount(val plate: String, val amount: Double)
    @Serializable
    data class LastMaintenance(val date: LocalDate, val plate: String)
}

@Serializable
data class ExpenseIndicators(
    val totalAmount: Double,
    val totalCount: Int,
    val mostCommonType: String? = null,
    val lastExpense: LastExpense? = null
) {
    @Serializable
    data class LastExpense(val date: LocalDate, val type: String, val description: String)
}

@Serializable
data class ExpenseReport(
    val distributions: Distributions,
    val summary: Summary
) {
    @Serializable
    data class Distributions(
        val byType: List<TypeDistribution>,
        val byVehicle: List<VehicleDistribution>,
        val byDriver: List<DriverDistribution>,
    ) {
        @Serializable
        data class TypeDistribution(val type: String, val totalAmount: Double, val totalCount: Int)
        @Serializable
        data class VehicleDistribution(val vehiclePlate: String, val totalAmount: Double, val totalCount: Int)
        @Serializable
        data class DriverDistribution(val driverName: String, val totalAmount: Double, val totalCount: Int)
    }

    @Serializable
    data class Summary(
        val totalAmount: Double,
        val totalCount: Int,
        val avgExpenseAmount: Double,
        val topExpenseType: TopExpenseType? = null,
        val topVehicleByAmount: TopVehicleAmount? = null,
        val topDriverByAmount: TopDriverAmount? = null,
        val lastExpense: LastExpense? = null
    ) {
        @Serializable
        data class TopExpenseType(val type: String, val totalAmount: Double)
        @Serializable
        data class TopVehicleAmount(val plate: String, val amount: Double)
        @Serializable
        data class TopDriverAmount(val name: String, val amount: Double)
        @Serializable
        data class LastExpense(val date: LocalDate, val type: String, val amount: Double)
    }
}