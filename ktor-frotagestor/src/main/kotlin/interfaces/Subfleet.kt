package com.frotagestor.interfaces

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Subfleet(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val vehicleCount: Int = 0,
    val activeVehicleCount: Int = 0
)

@Serializable
data class PartialSubfleet(
    val name: String? = null,
    val description: String? = null
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

@Serializable
data class SubfleetIndicators(
    val totalActive: Int,
    val totalVehicles: Int,
    val lastSubfleet: Subfleet? = null
)

@Serializable
data class SubfleetReportResponse(
    val period: Period,
    val summary: Summary,
    val subfleets: List<SubfleetMetric>
)

@Serializable
data class Period(
    val startDate: String,  // "2026-01-01"
    val endDate: String     // "2026-01-31"
)

@Serializable
data class Summary(
    val totalSubfleets: Int,
    val totalVehicles: Int,
    val overallEfficiency: Double  // R$/km médio geral (ex: 0.45)
)

@Serializable
data class SubfleetMetric(
    // Identificação
    val subfleetId: Int,
    val name: String,
    val color: String,        // Hex: "#10B981"
    val managerName: String,

    // Frota
    var totalVehicles: Int,
    var activeVehicles: Int,
    var maintenanceVehicles: Int,

    // Operacional
    val totalTrips: Int,
    val totalDistanceKm: Double,
    val totalExpenses: Double,

    // Métricas Derivadas
    val costPerKm: Double,          // totalExpenses / totalDistanceKm
    val tripsPerVehicle: Double,    // totalTrips / totalVehicles
    val kmPerTrip: Double,          // totalDistanceKm / totalTrips
    val vehiclesActiveRate: Double, // (activeVehicles / totalVehicles) * 100

    // Detalhes Ricos
    val topExpenseType: String,     // "Combustível", "Manutenção"
    val avgVehicleAge: Double,      // Anos

    val vehiclesByType: Map<String, Int>  // { "Caminhão": 4, "Carro": 3 }
)