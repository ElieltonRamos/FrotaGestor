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
    val odometer: Int? = null
)

@Serializable
data class PartialExpense(
    val id: Int? = null,
    val vehicleId: Int? = null,
    val driverId: Int? = null,
    val tripId: Int? = null,
    val date: LocalDate? = null,
    val type: String? = null,
    val description: String? = null,
    val amount: Double? = null,
    val liters: Double? = null,
    val pricePerLiter: Double? = null,
    val odometer: Int? = null
)
