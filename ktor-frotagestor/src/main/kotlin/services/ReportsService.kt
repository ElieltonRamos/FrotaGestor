package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.Message
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.VehicleReport
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ReportsService {
    suspend fun getReportVehicles(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<VehicleReport> = DatabaseFactory.dbQuery {

        val now = Clock.System.now().toLocalDateTime(TimeZone.Companion.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val timeZone = TimeZone.Companion.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)

        val sql = """
        SELECT
            v.id AS vehicle_id,
            v.plate,
            v.brand,
            v.`year`,
            v.status,
            COUNT(DISTINCT t.id) AS total_trips,
            COALESCE(SUM(t.distance_km),0) AS total_km,
            d.name AS top_driver_name,
            COALESCE(SUM(CASE WHEN e.type LIKE '%Combustível%' THEN e.amount ELSE 0 END),0) AS fuel_cost,
            COALESCE(SUM(CASE WHEN e.type LIKE '%Manutenção%' THEN e.amount ELSE 0 END),0) AS maintenance_cost,
            MAX(e.date) AS last_maintenance_date,
            COUNT(CASE WHEN t.status = 'EM_ANDAMENTO' THEN 1 END) > 0 AS is_in_use
        FROM vehicles v
        LEFT JOIN trips t ON v.id = t.vehicle_id AND t.start_time BETWEEN '$startDateTime' AND '$endDateTime'
        LEFT JOIN drivers d ON t.driver_id = d.id
        LEFT JOIN expenses e ON v.id = e.vehicle_id
        GROUP BY v.id, v.plate, v.brand, v.`year`, v.status, d.name
    """.trimIndent()

        val vehiclesData = mutableListOf<VehicleReport.UsageStats.TotalDistanceByVehicle>()
        val brands = mutableMapOf<String, Long>()
        val years = mutableMapOf<Int, Long>()
        val statuses = mutableMapOf<String, Long>()

        transaction {
            exec(sql) { rs ->
                while (rs.next()) {
                    val plate = rs.getString("plate")
                    val brand = rs.getString("brand") ?: "Desconhecida"
                    val year = rs.getInt("year")
                    val status = rs.getString("status") ?: "ATIVO"
                    val totalTrips = rs.getLong("total_trips")
                    val totalKm = rs.getDouble("total_km").toInt()
                    val fuelCost = rs.getDouble("fuel_cost")
                    val maintenanceCost = rs.getDouble("maintenance_cost")
                    val lastMaintenanceDate = rs.getString("last_maintenance_date")
                    val isInUse = rs.getBoolean("is_in_use")
                    val topDriverName = rs.getString("top_driver_name")

                    vehiclesData.add(
                        VehicleReport.UsageStats.TotalDistanceByVehicle(
                            plate = plate,
                            totalKm = totalKm,
                            totalTrips = totalTrips,
                            topDriver = if (topDriverName != null) VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver(
                                name = topDriverName,
                                trips = totalTrips.toInt()
                            ) else null,
                            fuelCost = fuelCost,
                            maintenanceCost = maintenanceCost,
                            totalCost = fuelCost + maintenanceCost,
                            lastMaintenanceDate = lastMaintenanceDate,
                            isInUse = isInUse
                        )
                    )

                    // Atualiza contagem de distribuições
                    brands[brand] = (brands[brand] ?: 0) + 1
                    years[year] = (years[year] ?: 0) + 1
                    statuses[status] = (statuses[status] ?: 0) + 1
                }
            }
        }

        val byBrand = brands.map { (brand, count) ->
            VehicleReport.Distributions.ByBrand(brand, count)
        }
        val byYear = years.map { (year, count) ->
            VehicleReport.Distributions.ByYear(year, count)
        }
        val byStatus = statuses.map { (statusStr, count) ->
            val status = when (statusStr) {
                "ATIVO" -> VehicleReport.Distributions.Status.ATIVO
                "MANUTENCAO" -> VehicleReport.Distributions.Status.MANUTENCAO
                "INATIVO" -> VehicleReport.Distributions.Status.INATIVO
                else -> VehicleReport.Distributions.Status.ATIVO
            }
            VehicleReport.Distributions.ByStatus(status, count)
        }

        val fuelConsumptionByVehicle = vehiclesData.map {
            VehicleReport.UsageStats.FuelConsumptionByVehicle(
                plate = it.plate,
                litersPerKm = if (it.totalKm > 0) it.fuelCost / it.totalKm else 0.0
            )
        }

        ServiceResponse(
            status = HttpStatusCode.Companion.OK,
            data = VehicleReport(
                distributions = VehicleReport.Distributions(
                    byBrand = byBrand,
                    byYear = byYear,
                    byStatus = byStatus
                ),
                usageStats = VehicleReport.UsageStats(
                    totalDistanceByVehicle = vehiclesData,
                    fuelConsumptionByVehicle = fuelConsumptionByVehicle
                )
            )
        )
    }
}