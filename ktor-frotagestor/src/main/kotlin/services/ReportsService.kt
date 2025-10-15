package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.DestinationDistribution
import com.frotagestor.interfaces.DriverDistribution
import com.frotagestor.interfaces.ExpenseReport
import com.frotagestor.interfaces.LastTrip
import com.frotagestor.interfaces.Message
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.StatusDistribution
import com.frotagestor.interfaces.TripDistributions
import com.frotagestor.interfaces.TripReport
import com.frotagestor.interfaces.TripStatus
import com.frotagestor.interfaces.VehicleDistribution
import com.frotagestor.interfaces.VehicleReport
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.transactions.transaction

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

    suspend fun getTripReport(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<TripReport> = DatabaseFactory.dbQuery {

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val timeZone = TimeZone.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)

        val sql = """
        SELECT
            t.status,
            v.plate AS vehicle_plate,
            d.name AS driver_name,
            t.end_location AS destination,
            COUNT(*) AS total_trips,
            COALESCE(SUM(t.distance_km), 0) AS total_distance
        FROM trips t
        LEFT JOIN vehicles v ON t.vehicle_id = v.id
        LEFT JOIN drivers d ON t.driver_id = d.id
        WHERE t.start_time BETWEEN '$startDateTime' AND '$endDateTime'
        GROUP BY t.status, v.plate, d.name, t.end_location
    """.trimIndent()

        val byStatus = mutableListOf<StatusDistribution>()
        val byVehicle = mutableListOf<VehicleDistribution>()
        val byDriver = mutableListOf<DriverDistribution>()
        val byDestination = mutableListOf<DestinationDistribution>()

        transaction {
            exec(sql) { rs ->
                while (rs.next()) {
                    val status = rs.getString("status")
                    val vehiclePlate = rs.getString("vehicle_plate") ?: "Desconhecido"
                    val driverName = rs.getString("driver_name") ?: "Desconhecido"
                    val destination = rs.getString("destination") ?: "Desconhecido"
                    val totalTrips = rs.getLong("total_trips").toInt()
                    val totalDistance = rs.getDouble("total_distance")

                    // Distribuições
                    byStatus.add(
                        StatusDistribution(
                            status = TripStatus.valueOf(status),
                            count = totalTrips
                        )
                    )

                    byVehicle.add(
                        VehicleDistribution(
                            vehiclePlate = vehiclePlate,
                            count = totalTrips,
                            totalCost = totalDistance // Se quiser custo real, precisa somar despesas associadas
                        )
                    )

                    byDriver.add(
                        DriverDistribution(
                            driverName = driverName,
                            count = totalTrips,
                            totalCost = totalDistance
                        )
                    )

                    byDestination.add(
                        DestinationDistribution(
                            destination = destination,
                            totalTrips = totalTrips,
                            totalCost = totalDistance
                        )
                    )
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = TripReport(
                distributions = TripDistributions(
                    byStatus = byStatus,
                    byVehicle = byVehicle,
                    byDriver = byDriver,
                    byDestination = byDestination
                ),
            )
        )
    }

    suspend fun getReportExpenses(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<ExpenseReport> = DatabaseFactory.dbQuery {
        // Define datas padrão: mês atual
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val timeZone = TimeZone.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)

        val sql = """
        SELECT
            e.type AS expense_type,
            COALESCE(v.plate, 'Desconhecido') AS vehicle_plate,
            COALESCE(d.name, 'Desconhecido') AS driver_name,
            COUNT(e.id) AS total_count,
            COALESCE(SUM(e.amount), 0) AS total_amount,
            (SELECT type FROM expenses e2
             WHERE e2.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY e2.type ORDER BY SUM(e2.amount) DESC LIMIT 1) AS top_expense_type,
            (SELECT SUM(e3.amount) FROM expenses e3
             WHERE e3.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY e3.type ORDER BY SUM(e3.amount) DESC LIMIT 1) AS top_expense_amount,
            (SELECT v2.plate FROM vehicles v2
             JOIN expenses e4 ON v2.id = e4.vehicle_id
             WHERE e4.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v2.id ORDER BY SUM(e4.amount) DESC LIMIT 1) AS top_vehicle_plate,
            (SELECT SUM(e5.amount) FROM expenses e5
             JOIN vehicles v5 ON v5.id = e5.vehicle_id
             WHERE e5.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v5.id ORDER BY SUM(e5.amount) DESC LIMIT 1) AS top_vehicle_amount,
            (SELECT d2.name FROM drivers d2
             JOIN expenses e6 ON d2.id = e6.driver_id
             WHERE e6.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY d2.id ORDER BY SUM(e6.amount) DESC LIMIT 1) AS top_driver_name,
            (SELECT SUM(e7.amount) FROM expenses e7
             JOIN drivers d7 ON d7.id = e7.driver_id
             WHERE e7.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY d7.id ORDER BY SUM(e7.amount) DESC LIMIT 1) AS top_driver_amount,
            (SELECT e8.date FROM expenses e8
             WHERE e8.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e8.date DESC LIMIT 1) AS last_expense_date,
            (SELECT e9.type FROM expenses e9
             WHERE e9.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e9.date DESC LIMIT 1) AS last_expense_type,
            (SELECT e10.amount FROM expenses e10
             WHERE e10.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e10.date DESC LIMIT 1) AS last_expense_amount
        FROM expenses e
        LEFT JOIN vehicles v ON e.vehicle_id = v.id
        LEFT JOIN drivers d ON e.driver_id = d.id
        WHERE e.date BETWEEN '$startDateTime' AND '$endDateTime'
        GROUP BY e.type, v.plate, d.name
    """.trimIndent()

        // Mapas para agrupar resultados
        val typeMap = mutableMapOf<String, Pair<Double, Int>>()
        val vehicleMap = mutableMapOf<String, Pair<Double, Int>>()
        val driverMap = mutableMapOf<String, Pair<Double, Int>>()
        var totalAmount = 0.0
        var totalCount = 0
        var topExpenseType: ExpenseReport.Summary.TopExpenseType? = null
        var topVehicleByAmount: ExpenseReport.Summary.TopVehicleAmount? = null
        var topDriverByAmount: ExpenseReport.Summary.TopDriverAmount? = null
        var lastExpense: ExpenseReport.Summary.LastExpense? = null

        transaction {
            exec(sql) { rs ->
                while (rs.next()) {
                    val expenseType = rs.getString("expense_type") ?: "OUTROS"
                    val vehiclePlate = rs.getString("vehicle_plate") ?: "Desconhecido"
                    val driverName = rs.getString("driver_name") ?: "Desconhecido"
                    val totalCountRow = rs.getInt("total_count")
                    val totalAmountRow = rs.getDouble("total_amount")

                    // Acumula totais para o sumário
                    totalCount += totalCountRow
                    totalAmount += totalAmountRow

                    // Agrupa por tipo
                    val typePair = typeMap[expenseType] ?: Pair(0.0, 0)
                    typeMap[expenseType] = Pair(
                        typePair.first + totalAmountRow,
                        typePair.second + totalCountRow
                    )

                    // Agrupa por veículo
                    val vehiclePair = vehicleMap[vehiclePlate] ?: Pair(0.0, 0)
                    vehicleMap[vehiclePlate] = Pair(
                        vehiclePair.first + totalAmountRow,
                        vehiclePair.second + totalCountRow
                    )

                    // Agrupa por motorista
                    val driverPair = driverMap[driverName] ?: Pair(0.0, 0)
                    driverMap[driverName] = Pair(
                        driverPair.first + totalAmountRow,
                        driverPair.second + totalCountRow
                    )

                    // Dados do sumário
                    val topExpenseTypeStr = rs.getString("top_expense_type")
                    val topExpenseAmount = rs.getDouble("top_expense_amount")
                    val topVehiclePlate = rs.getString("top_vehicle_plate")
                    val topVehicleAmount = rs.getDouble("top_vehicle_amount")
                    val topDriverName = rs.getString("top_driver_name")
                    val topDriverAmount = rs.getDouble("top_driver_amount")
                    val lastExpenseDate = rs.getString("last_expense_date")
                    val lastExpenseType = rs.getString("last_expense_type")
                    val lastExpenseAmount = rs.getDouble("last_expense_amount")

                    if (topExpenseTypeStr != null) {
                        topExpenseType = ExpenseReport.Summary.TopExpenseType(
                            type = topExpenseTypeStr,
                            totalAmount = topExpenseAmount
                        )
                    }

                    if (topVehiclePlate != null) {
                        topVehicleByAmount = ExpenseReport.Summary.TopVehicleAmount(
                            plate = topVehiclePlate,
                            amount = topVehicleAmount
                        )
                    }

                    if (topDriverName != null) {
                        topDriverByAmount = ExpenseReport.Summary.TopDriverAmount(
                            name = topDriverName,
                            amount = topDriverAmount
                        )
                    }

                    if (lastExpenseDate != null && lastExpenseType != null) {
                        lastExpense = ExpenseReport.Summary.LastExpense(
                            date = LocalDate.parse(lastExpenseDate),
                            type = lastExpenseType,
                            amount = lastExpenseAmount
                        )
                    }
                }
            }
        }

        // Converte mapas para listas
        val byType = typeMap.map { (type, pair) ->
            ExpenseReport.Distributions.TypeDistribution(
                type = type,
                totalAmount = pair.first,
                totalCount = pair.second
            )
        }
        val byVehicle = vehicleMap.map { (plate, pair) ->
            ExpenseReport.Distributions.VehicleDistribution(
                vehiclePlate = plate,
                totalAmount = pair.first,
                totalCount = pair.second
            )
        }
        val byDriver = driverMap.map { (name, pair) ->
            ExpenseReport.Distributions.DriverDistribution(
                driverName = name,
                totalAmount = pair.first,
                totalCount = pair.second
            )
        }

        val avgExpenseAmount = if (totalCount > 0) totalAmount / totalCount else 0.0

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = ExpenseReport(
                distributions = ExpenseReport.Distributions(
                    byType = byType,
                    byVehicle = byVehicle,
                    byDriver = byDriver
                ),
                summary = ExpenseReport.Summary(
                    totalAmount = totalAmount,
                    totalCount = totalCount,
                    avgExpenseAmount = avgExpenseAmount,
                    topExpenseType = topExpenseType,
                    topVehicleByAmount = topVehicleByAmount,
                    topDriverByAmount = topDriverByAmount,
                    lastExpense = lastExpense
                )
            )
        )
    }
}