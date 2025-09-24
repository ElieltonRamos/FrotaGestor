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