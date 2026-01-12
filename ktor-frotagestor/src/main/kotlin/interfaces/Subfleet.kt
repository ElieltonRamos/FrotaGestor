package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
enum class SubfleetStatus {
    ACTIVE,
    INACTIVE
}

@Serializable
data class Subfleet(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val parentId: Int? = null,
    val color: String = "#3B82F6",
    val icon: String = "truck",
    val managerUserId: Int? = null,
    val status: SubfleetStatus = SubfleetStatus.ACTIVE,

    // Campos de JOIN (não persistidos)
    val parentName: String? = null,      // Nome da subfrota pai
    val managerName: String? = null,     // Nome do gerente
    val vehicleCount: Int = 0,           // Contagem de veículos
    val activeVehicleCount: Int = 0,     // Veículos ativos

    // Metadata
    val createdAt: LocalDateTime? = null
)

@Serializable
data class PartialSubfleet(
    val name: String? = null,
    val description: String? = null,
    val parentId: Int? = null,
    val color: String? = null,
    val icon: String? = null,
    val managerUserId: Int? = null,
    val status: SubfleetStatus? = null
)

@Serializable
data class SubfleetReport(
    val subfleetId: Int,
    val totalVehicles: Int,
    val activeVehicles: Int,
    val maintenanceVehicles: Int,
    val totalTrips: Int,
    val totalDistanceKm: Double,
    val totalExpenses: Double
)
