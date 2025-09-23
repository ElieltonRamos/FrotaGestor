package com.frotagestor.interfaces

data class Vehicle(
    val id: Int? = null,
    val plate: String,
    val model: String,
    val brand: String? = null,
    val year: Int? = null,
    val status: VehicleStatus = VehicleStatus.Active
)

enum class VehicleStatus {
    Active,
    Inactive,
    Maintenance
}