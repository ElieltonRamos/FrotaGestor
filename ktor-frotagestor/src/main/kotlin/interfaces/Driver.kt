package com.frotagestor.interfaces

import java.time.LocalDate

data class Driver(
    val id: Int? = null,
    val name: String,
    val cpf: String,
    val cnh: String,
    val cnhCategory: String? = null,
    val cnhExpiration: LocalDate? = null,
    val phone: String? = null,
    val email: String? = null,
    val status: DriverStatus
)

enum class DriverStatus {
    Ativo,
    Inativo
}
