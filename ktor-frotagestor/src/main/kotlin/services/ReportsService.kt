package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.DatabaseFactory.dbQuery
import com.frotagestor.database.models.ExpensesTable
import com.frotagestor.database.models.SubfleetsTable
import com.frotagestor.database.models.TripsTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.DestinationDistribution
import com.frotagestor.interfaces.DriverDistribution
import com.frotagestor.interfaces.DriverIndicators
import com.frotagestor.interfaces.DriverReport
import com.frotagestor.interfaces.ExpenseReport
import com.frotagestor.interfaces.LastTrip
import com.frotagestor.interfaces.Message
import com.frotagestor.interfaces.Period
import com.frotagestor.interfaces.Summary
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.StatusDistribution
import com.frotagestor.interfaces.SubfleetMetric
import com.frotagestor.interfaces.SubfleetReport
import com.frotagestor.interfaces.SubfleetReportResponse
import com.frotagestor.interfaces.TripDistributions
import com.frotagestor.interfaces.TripReport
import com.frotagestor.interfaces.TripStatus
import com.frotagestor.interfaces.VehicleDistribution
import com.frotagestor.interfaces.VehicleReport
import com.frotagestor.interfaces.VehicleStatus
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
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
            v.`model_year`,
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
        GROUP BY v.id, v.plate, v.brand, v.`model_year`, v.status, d.name
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
                    val year = rs.getInt("model_year")
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

        // Maps para agregar os dados
        val statusMap = mutableMapOf<TripStatus, Int>()
        val vehicleMap = mutableMapOf<String, Pair<Int, Double>>() // count, totalCost
        val driverMap = mutableMapOf<String, Pair<Int, Double>>()
        val destinationMap = mutableMapOf<String, Pair<Int, Double>>()

        transaction {
            // Query para Status
            val statusSql = """
            SELECT status, COUNT(*) as total_trips
            FROM trips
            WHERE start_time >= '$startDateTime' AND start_time < '$endDateTime'
            GROUP BY status
        """.trimIndent()

            exec(statusSql) { rs ->
                while (rs.next()) {
                    val status = TripStatus.valueOf(rs.getString("status"))
                    val count = rs.getInt("total_trips")
                    statusMap[status] = count
                }
            }

            // Query para Veículos
            val vehicleSql = """
            SELECT 
                COALESCE(v.plate, 'Desconhecido') AS vehicle_plate,
                COUNT(*) AS total_trips,
                COALESCE(SUM(e.amount), 0) AS total_cost
            FROM trips t
            LEFT JOIN vehicles v ON t.vehicle_id = v.id
            LEFT JOIN expenses e ON e.trip_id = t.id
            WHERE t.start_time >= '$startDateTime' AND t.start_time < '$endDateTime'
            GROUP BY v.plate
        """.trimIndent()

            exec(vehicleSql) { rs ->
                while (rs.next()) {
                    val plate = rs.getString("vehicle_plate")
                    val count = rs.getInt("total_trips")
                    val cost = rs.getDouble("total_cost")
                    vehicleMap[plate] = Pair(count, cost)
                }
            }

            // Query para Motoristas
            val driverSql = """
            SELECT 
                COALESCE(d.name, 'Desconhecido') AS driver_name,
                COUNT(*) AS total_trips,
                COALESCE(SUM(e.amount), 0) AS total_cost
            FROM trips t
            LEFT JOIN drivers d ON t.driver_id = d.id
            LEFT JOIN expenses e ON e.trip_id = t.id
            WHERE t.start_time >= '$startDateTime' AND t.start_time < '$endDateTime'
            GROUP BY d.name
        """.trimIndent()

            exec(driverSql) { rs ->
                while (rs.next()) {
                    val name = rs.getString("driver_name")
                    val count = rs.getInt("total_trips")
                    val cost = rs.getDouble("total_cost")
                    driverMap[name] = Pair(count, cost)
                }
            }

            // Query para Destinos
            val destinationSql = """
            SELECT 
                COALESCE(t.end_location, 'Desconhecido') AS destination,
                COUNT(*) AS total_trips,
                COALESCE(SUM(e.amount), 0) AS total_cost
            FROM trips t
            LEFT JOIN expenses e ON e.trip_id = t.id
            WHERE t.start_time >= '$startDateTime' AND t.start_time < '$endDateTime'
            GROUP BY t.end_location
        """.trimIndent()

            exec(destinationSql) { rs ->
                while (rs.next()) {
                    val destination = rs.getString("destination")
                    val count = rs.getInt("total_trips")
                    val cost = rs.getDouble("total_cost")
                    destinationMap[destination] = Pair(count, cost)
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = TripReport(
                distributions = TripDistributions(
                    byStatus = statusMap.map { (status, count) ->
                        StatusDistribution(status = status, count = count)
                    },
                    byVehicle = vehicleMap.map { (plate, data) ->
                        VehicleDistribution(
                            vehiclePlate = plate,
                            count = data.first,
                            totalCost = data.second
                        )
                    },
                    byDriver = driverMap.map { (name, data) ->
                        DriverDistribution(
                            driverName = name,
                            count = data.first,
                            totalCost = data.second
                        )
                    },
                    byDestination = destinationMap.map { (destination, data) ->
                        DestinationDistribution(
                            destination = destination,
                            totalTrips = data.first,
                            totalCost = data.second
                        )
                    }
                )
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

    suspend fun getDriverReport(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<DriverReport> = DatabaseFactory.dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val timeZone = TimeZone.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
        val thirtyDaysFromNow = now.plus(DatePeriod(days = 30))

        val sql = """
    SELECT
        d.id AS driver_id,
        d.name AS driver_name,
        d.cnh_category AS driver_category, -- Include individual driver's cnh_category
        COUNT(DISTINCT t.id) AS total_trips,
        COALESCE(SUM(t.distance_km), 0) AS total_distance,
        COALESCE(SUM(e.amount), 0) AS total_cost,
        COALESCE(SUM(e.liters), 0) AS total_liters,
        MAX(t.end_time) AS last_trip_date,
        (SELECT COUNT(*) 
         FROM drivers d2 
         WHERE d2.deleted_at IS NULL AND d2.status = 'ATIVO') AS total_drivers,
        (SELECT COUNT(*) 
         FROM drivers d3 
         WHERE d3.cnh_expiration < '$now' AND d3.deleted_at IS NULL AND d3.status = 'ATIVO') AS cnh_expired,
        (SELECT COUNT(*) 
         FROM drivers d4 
         WHERE d4.cnh_expiration BETWEEN '$now' AND '$thirtyDaysFromNow' 
         AND d4.deleted_at IS NULL AND d4.status = 'ATIVO') AS cnh_expiring_soon,
        (SELECT d5.cnh_category 
         FROM drivers d5 
         WHERE d5.cnh_category IS NOT NULL AND d5.deleted_at IS NULL AND d5.status = 'ATIVO'
         GROUP BY d5.cnh_category 
         ORDER BY COUNT(*) DESC LIMIT 1) AS most_common_category
    FROM drivers d
    LEFT JOIN trips t ON d.id = t.driver_id AND t.start_time BETWEEN '$startDateTime' AND '$endDateTime'
    LEFT JOIN expenses e ON d.id = e.driver_id AND e.date BETWEEN '$startDateTime' AND '$endDateTime'
    WHERE d.deleted_at IS NULL AND d.status = 'ATIVO'
    GROUP BY d.id, d.name, d.cnh_category
""".trimIndent()

        // Mapas para agrupar resultados
        val driversStats = mutableListOf<DriverReport.DriverStats>()
        val categoryMap = mutableMapOf<String, Int>()
        var totalDrivers = 0
        var cnhExpired = 0
        var cnhExpiringSoon = 0
        var mostCommonCategory: String? = null

        transaction {
            exec(sql) { rs ->
                while (rs.next()) {
                    val driverId = rs.getInt("driver_id")
                    val driverName = rs.getString("driver_name") ?: "Desconhecido"
                    val totalTrips = rs.getInt("total_trips")
                    val totalDistance = rs.getDouble("total_distance")
                    val totalCost = rs.getDouble("total_cost")
                    val totalLiters = rs.getDouble("total_liters")
                    val lastTripDate = rs.getString("last_trip_date")
                    val averageFuelConsumption = if (totalDistance > 0) totalLiters / totalDistance else null

                    // Adiciona às estatísticas de motoristas
                    driversStats.add(
                        DriverReport.DriverStats(
                            driverName = driverName,
                            driverId = driverId,
                            totalTrips = totalTrips,
                            totalDistance = totalDistance,
                            totalCost = totalCost,
                            averageFuelConsumption = averageFuelConsumption,
                            lastTripDate = lastTripDate
                        )
                    )

                    // Acumula dados de distribuição por categoria
                    val category = rs.getString("driver_category")
                    if (category != null) {
                        categoryMap[category] = (categoryMap[category] ?: 0) + 1
                    }

                    // Dados de distribuição geral (capturados uma vez)
                    if (totalDrivers == 0) {
                        totalDrivers = rs.getInt("total_drivers")
                        cnhExpired = rs.getInt("cnh_expired")
                        cnhExpiringSoon = rs.getInt("cnh_expiring_soon")
                        mostCommonCategory = rs.getString("most_common_category")
                    }
                }
            }
        }

        // Converte o mapa de categorias para lista
        val byCategory = categoryMap.map { (category, count) ->
            DriverReport.Distributions.ByCategory(
                category = category,
                count = count
            )
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = DriverReport(
                distributions = DriverReport.Distributions(
                    totalDrivers = totalDrivers,
                    cnhExpiringSoon = cnhExpiringSoon,
                    cnhExpired = cnhExpired,
                    byCategory = byCategory
                ),
                driversStats = driversStats
            )
        )
    }

    suspend fun getSubfleetReport(subfleetId: Int): ServiceResponse<SubfleetReport> = DatabaseFactory.dbQuery {
        val vehicles = VehiclesTable
            .selectAll()
            .where {
                (VehiclesTable.subfleetId eq subfleetId) and
                        (VehiclesTable.deletedAt.isNull())
            }
            .toList()

        val vehicleIds = vehicles.map { it[VehiclesTable.id] }

        val totalVehicles = vehicles.size
        val activeVehicles = vehicles.count { it[VehiclesTable.status] == VehicleStatus.ATIVO }
        val maintenanceVehicles = vehicles.count { it[VehiclesTable.status] == VehicleStatus.MANUTENCAO }

        // Contagens de trips (se table existir)
        val totalTrips = try {
            TripsTable
                .selectAll()
                .where { TripsTable.vehicleId inList vehicleIds }
                .count()
                .toInt()
        } catch (e: Exception) {
            0
        }

        // Distância total (se table existir)
        val totalDistanceKm = try {
            TripsTable
                .select(TripsTable.distanceKm.sum())
                .where { TripsTable.vehicleId inList vehicleIds }
                .singleOrNull()
                ?.get(TripsTable.distanceKm.sum())
                ?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }

        // Despesas totais (se table existir)
        val totalExpenses = try {
            ExpensesTable
                .select(ExpensesTable.amount.sum())
                .where { ExpensesTable.vehicleId inList vehicleIds }
                .singleOrNull()
                ?.get(ExpensesTable.amount.sum())
                ?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }

        val report = SubfleetReport(
            subfleetId = subfleetId,
            totalVehicles = totalVehicles,
            activeVehicles = activeVehicles,
            maintenanceVehicles = maintenanceVehicles,
            totalTrips = totalTrips,
            totalDistanceKm = totalDistanceKm,
            totalExpenses = totalExpenses
        )

        ServiceResponse(HttpStatusCode.OK, report)
    }

    suspend fun getReportAllSubfleets(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<SubfleetReportResponse> = DatabaseFactory.dbQuery {

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: now

        // Convertendo LocalDate para String para usar na query
        val startDateStr = start.toString()
        val endDateStr = end.plus(DatePeriod(days = 1)).toString()

        val subfleetsData = mutableMapOf<Int, MutableMap<String, Any>>()
        val vehicleTypesBySubfleet = mutableMapOf<Int, MutableMap<String, Int>>()

        transaction {
            // Query principal - usando interpolação segura
            val mainQuery = """
            SELECT
                s.id AS subfleet_id,
                s.name AS subfleet_name,
                (SELECT COUNT(*) FROM vehicles v WHERE v.subfleet_id = s.id AND v.deleted_at IS NULL) AS total_vehicles,
                (SELECT COUNT(*) FROM vehicles v WHERE v.subfleet_id = s.id AND v.status = 'ATIVO' AND v.deleted_at IS NULL) AS active_vehicles,
                (SELECT COUNT(*) FROM vehicles v WHERE v.subfleet_id = s.id AND v.status = 'MANUTENCAO' AND v.deleted_at IS NULL) AS maintenance_vehicles,
                (SELECT COUNT(DISTINCT t.id) FROM trips t 
                 WHERE t.vehicle_id IN (SELECT id FROM vehicles WHERE subfleet_id = s.id AND deleted_at IS NULL)
                 AND t.start_time >= '$startDateStr' 
                 AND t.start_time < '$endDateStr') AS total_trips,
                (SELECT COALESCE(SUM(t.distance_km), 0) FROM trips t 
                 WHERE t.vehicle_id IN (SELECT id FROM vehicles WHERE subfleet_id = s.id AND deleted_at IS NULL)
                 AND t.start_time >= '$startDateStr' 
                 AND t.start_time < '$endDateStr') AS total_distance_km,
                (SELECT COALESCE(SUM(e.amount), 0) FROM expenses e 
                 WHERE e.vehicle_id IN (SELECT id FROM vehicles WHERE subfleet_id = s.id AND deleted_at IS NULL)
                 AND e.date >= '$startDateStr' 
                 AND e.date < '$endDateStr') AS total_expenses,
                (SELECT COALESCE(AVG(YEAR(CURRENT_DATE()) - v.model_year), 0) 
                 FROM vehicles v WHERE v.subfleet_id = s.id AND v.deleted_at IS NULL 
                 AND v.model_year IS NOT NULL) AS avg_vehicle_age,
                (SELECT e2.type FROM expenses e2 
                 WHERE e2.vehicle_id IN (SELECT id FROM vehicles WHERE subfleet_id = s.id AND deleted_at IS NULL)
                 AND e2.date >= '$startDateStr' 
                 AND e2.date < '$endDateStr'
                 GROUP BY e2.type 
                 ORDER BY SUM(e2.amount) DESC 
                 LIMIT 1) AS top_expense_type
            FROM subfleets s
        """.trimIndent()

            // Executa query principal
            exec(mainQuery) { rs ->
                while (rs.next()) {
                    val subfleetId = rs.getInt("subfleet_id")
                    subfleetsData[subfleetId] = mutableMapOf(
                        "subfleet_id" to subfleetId,
                        "name" to (rs.getString("subfleet_name") ?: "Sem Nome"),
                        "total_vehicles" to rs.getInt("total_vehicles"),
                        "active_vehicles" to rs.getInt("active_vehicles"),
                        "maintenance_vehicles" to rs.getInt("maintenance_vehicles"),
                        "total_trips" to rs.getInt("total_trips"),
                        "total_distance_km" to rs.getDouble("total_distance_km"),
                        "total_expenses" to rs.getDouble("total_expenses"),
                        "avg_vehicle_age" to rs.getDouble("avg_vehicle_age"),
                        "top_expense_type" to (rs.getString("top_expense_type") ?: "N/A")
                    )
                    vehicleTypesBySubfleet[subfleetId] = mutableMapOf()
                }
            }

            // Query de modelos
            val modelsQuery = """
            SELECT
                v.subfleet_id,
                COALESCE(v.model, 'Desconhecido') AS vehicle_model,
                COUNT(*) AS model_count
            FROM vehicles v
            WHERE v.deleted_at IS NULL
            GROUP BY v.subfleet_id, v.model
        """.trimIndent()

            exec(modelsQuery) { rs ->
                while (rs.next()) {
                    val subfleetId = rs.getInt("subfleet_id")
                    if (subfleetId != 0) {
                        val vehicleModel = rs.getString("vehicle_model")
                        vehicleTypesBySubfleet[subfleetId]?.let {
                            it[vehicleModel] = rs.getInt("model_count")
                        }
                    }
                }
            }
        }

        // Montagem dos resultados
        val subfleetMetrics = subfleetsData.map { (subfleetId, data) ->
            val totalVehicles = data["total_vehicles"] as Int
            val activeVehicles = data["active_vehicles"] as Int
            val totalTrips = data["total_trips"] as Int
            val totalDistanceKm = data["total_distance_km"] as Double
            val totalExpenses = data["total_expenses"] as Double

            SubfleetMetric(
                subfleetId = subfleetId,
                name = data["name"] as String,
                color = "#${String.format("%06X", (subfleetId * 123456) and 0xFFFFFF)}",
                managerName = "",
                totalVehicles = totalVehicles,
                activeVehicles = activeVehicles,
                maintenanceVehicles = data["maintenance_vehicles"] as Int,
                totalTrips = totalTrips,
                totalDistanceKm = totalDistanceKm,
                totalExpenses = totalExpenses,
                costPerKm = if (totalDistanceKm > 0) totalExpenses / totalDistanceKm else 0.0,
                tripsPerVehicle = if (totalVehicles > 0) totalTrips.toDouble() / totalVehicles else 0.0,
                kmPerTrip = if (totalTrips > 0) totalDistanceKm / totalTrips else 0.0,
                vehiclesActiveRate = if (totalVehicles > 0) (activeVehicles.toDouble() / totalVehicles) * 100 else 0.0,
                topExpenseType = data["top_expense_type"] as String,
                avgVehicleAge = data["avg_vehicle_age"] as Double,
                vehiclesByType = vehicleTypesBySubfleet[subfleetId] ?: emptyMap()
            )
        }

        val totalSubfleets = subfleetMetrics.size
        val totalVehicles = subfleetMetrics.sumOf { it.totalVehicles }
        val overallTotalExpenses = subfleetMetrics.sumOf { it.totalExpenses }
        val overallTotalDistance = subfleetMetrics.sumOf { it.totalDistanceKm }
        val overallEfficiency = if (overallTotalDistance > 0) overallTotalExpenses / overallTotalDistance else 0.0

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = SubfleetReportResponse(
                period = Period(startDate = start.toString(), endDate = end.toString()),
                summary = Summary(
                    totalSubfleets = totalSubfleets,
                    totalVehicles = totalVehicles,
                    overallEfficiency = overallEfficiency
                ),
                subfleets = subfleetMetrics
            )
        )
    }

}