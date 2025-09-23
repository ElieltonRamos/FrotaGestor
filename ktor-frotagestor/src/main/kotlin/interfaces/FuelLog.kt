package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

data class FuelLog(
    val id: Int? = null,
    val vehicleId: Int,
    val driverId: Int? = null,
    val date: LocalDateTime,
    val liters: BigDecimal,
    val cost: BigDecimal,
    val station: String? = null
)