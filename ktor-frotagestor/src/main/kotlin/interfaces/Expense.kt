package com.frotagestor.interfaces

import java.math.BigDecimal
import java.time.LocalDate

data class Expense(
    val id: Int? = null,
    val vehicleId: Int? = null,
    val driverId: Int? = null,
    val tripId: Int? = null,
    val date: LocalDate,
    val type: String,
    val description: String? = null,
    val amount: BigDecimal
)