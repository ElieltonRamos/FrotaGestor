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
