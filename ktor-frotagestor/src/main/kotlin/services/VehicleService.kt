package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.DriversTable
import com.frotagestor.database.models.ExpensesTable
import com.frotagestor.database.models.TripsTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateVehicle
import com.frotagestor.validations.validatePartialVehicle
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

class VehicleService {

    suspend fun createVehicle(req: String): ServiceResponse<Message> {
        val newVehicle = validateVehicle(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingVehicle = DatabaseFactory.dbQuery {
            VehiclesTable
                .selectAll()
                .where { VehiclesTable.plate eq newVehicle.plate }
                .singleOrNull()
        }

        if (existingVehicle != null) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Veículo já registrado!")
            )
        }

        DatabaseFactory.dbQuery {
            VehiclesTable.insert {
                it[plate] = newVehicle.plate
                it[model] = newVehicle.model
                it[brand] = newVehicle.brand
                it[year] = newVehicle.year
                it[status] = newVehicle.status
                it[deletedAt] = null
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Veículo criado com sucesso")
        )
    }

    suspend fun updateVehicle(id: Int, req: String): ServiceResponse<Message> {
        val updatedVehicle = validatePartialVehicle(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingVehicle = DatabaseFactory.dbQuery {
            VehiclesTable
                .selectAll()
                .where { VehiclesTable.id eq id and VehiclesTable.deletedAt.isNull() }
                .singleOrNull()
        }

        if (existingVehicle == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Veículo não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            VehiclesTable.update({ VehiclesTable.id eq id }) {
                updatedVehicle.plate?.let { p -> it[plate] = p }
                updatedVehicle.model?.let { m -> it[model] = m }
                updatedVehicle.brand?.let { b -> it[brand] = b }
                updatedVehicle.year?.let { y -> it[year] = y }
                updatedVehicle.status?.let { s -> it[status] = s }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Veículo atualizado com sucesso")
        )
    }

    suspend fun getAllVehicles(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = VehiclesTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        plateFilter: String? = null,
        modelFilter: String? = null,
        brandFilter: String? = null,
        yearFilter: Int? = null,
        statusFilter: VehicleStatus? = null
    ): ServiceResponse<PaginatedResponse<Vehicle>> {
        return DatabaseFactory.dbQuery {
            val query = VehiclesTable
                .selectAll()
                .apply {
                    if (statusFilter != VehicleStatus.INATIVO) {
                        andWhere { VehiclesTable.deletedAt.isNull() }
                    }
                    if (idFilter != null) {
                        andWhere { VehiclesTable.id eq idFilter }
                    }
                    if (!plateFilter.isNullOrBlank()) {
                        andWhere { VehiclesTable.plate like "%$plateFilter%" }
                    }
                    if (!modelFilter.isNullOrBlank()) {
                        andWhere { VehiclesTable.model like "%$modelFilter%" }
                    }
                    if (!brandFilter.isNullOrBlank()) {
                        andWhere { VehiclesTable.brand like "%$brandFilter%" }
                    }
                    if (yearFilter != null) {
                        andWhere { VehiclesTable.year eq yearFilter }
                    }
                    if (statusFilter != null) {
                        andWhere { VehiclesTable.status eq statusFilter }
                    }
                }

            val total = query.count()

            val orderExpr = if (sortBy == VehiclesTable.model || sortBy == VehiclesTable.brand) {
                CustomFunction<String>("UPPER", TextColumnType(), sortBy as Column<String>)
            } else {
                sortBy
            }

            val results = query
                .orderBy(orderExpr to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Vehicle(
                        id = it[VehiclesTable.id],
                        plate = it[VehiclesTable.plate],
                        model = it[VehiclesTable.model],
                        brand = it[VehiclesTable.brand],
                        year = it[VehiclesTable.year],
                        status = it[VehiclesTable.status],
                        iconMapUrl = it[VehiclesTable.iconMapUrl]
                    )
                }

            ServiceResponse(
                status = HttpStatusCode.OK,
                data = PaginatedResponse(
                    data = results,
                    total = total.toInt(),
                    page = page,
                    limit = limit,
                    totalPages = if (total == 0L) 0 else ((total + limit - 1) / limit).toInt()
                )
            )
        }
    }

    suspend fun findVehicleById(id: Int): ServiceResponse<Any> {
        val vehicle = DatabaseFactory.dbQuery {
            VehiclesTable
                .selectAll()
                .where { VehiclesTable.id eq id and VehiclesTable.deletedAt.isNull() }
                .singleOrNull()
                ?.let {
                    Vehicle(
                        id = it[VehiclesTable.id],
                        plate = it[VehiclesTable.plate],
                        model = it[VehiclesTable.model],
                        brand = it[VehiclesTable.brand],
                        year = it[VehiclesTable.year],
                        status = it[VehiclesTable.status],
                        iconMapUrl = it[VehiclesTable.iconMapUrl]
                    )
                }
        }

        return if (vehicle == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = mapOf("message" to "Veículo não encontrado")
            )
        } else {
            ServiceResponse(
                status = HttpStatusCode.OK,
                data = vehicle
            )
        }
    }

    suspend fun getIndicators(): ServiceResponse<VehicleIndicators> {
        return DatabaseFactory.dbQuery {
            val activeCount = VehiclesTable
                .selectAll()
                .where { VehiclesTable.status eq VehicleStatus.ATIVO and VehiclesTable.deletedAt.isNull() }
                .count()

            val maintenanceCount = VehiclesTable
                .selectAll()
                .where { VehiclesTable.status eq VehicleStatus.MANUTENCAO and VehiclesTable.deletedAt.isNull() }
                .count()

            // Último veículo cadastrado (maior ID)
            val lastVehicleRow = VehiclesTable
                .selectAll().where { VehiclesTable.deletedAt.isNull() }
                .orderBy(VehiclesTable.id, SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            val lastVehicle = lastVehicleRow?.let {
                Vehicle(
                    id = it[VehiclesTable.id],
                    plate = it[VehiclesTable.plate],
                    model = it[VehiclesTable.model],
                    brand = it[VehiclesTable.brand],
                    year = it[VehiclesTable.year],
                    status = it[VehiclesTable.status],
                    iconMapUrl = it[VehiclesTable.iconMapUrl]
                )
            }

            ServiceResponse(
                status = HttpStatusCode.OK,
                data = VehicleIndicators(
                    active = activeCount.toInt(),
                    maintenance = maintenanceCount.toInt(),
                    lastVehicle = lastVehicle
                )
            )
        }
    }

    suspend fun getReport(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<VehicleReport> = DatabaseFactory.dbQuery {

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val timeZone = TimeZone.currentSystemDefault()
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
            status = HttpStatusCode.OK,
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

//    ⏱ Vehicle report service execution time: 297ms


//    suspend fun getReport(
//        startDate: LocalDate? = null,
//        endDate: LocalDate? = null
//    ): ServiceResponse<VehicleReport> = DatabaseFactory.dbQuery {
//
//        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//        val start = startDate ?: LocalDate(now.year, now.month, 1)
//        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
//        val timeZone = TimeZone.currentSystemDefault()
//        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//
//        // --- Distribuições ---
//        val byBrand = VehiclesTable
//            .select(VehiclesTable.brand, VehiclesTable.brand.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.brand)
//            .map { VehicleReport.Distributions.ByBrand(it[VehiclesTable.brand] ?: "Desconhecida", it[VehiclesTable.brand.count()]) }
//
//        val byYear = VehiclesTable
//            .select(VehiclesTable.year, VehiclesTable.year.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.year)
//            .map { VehicleReport.Distributions.ByYear(it[VehiclesTable.year] ?: 0, it[VehiclesTable.year.count()]) }
//
//        val byStatus = VehiclesTable
//            .select(VehiclesTable.status, VehiclesTable.status.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.status)
//            .map { VehicleReport.Distributions.ByStatus(
//                status = when(it[VehiclesTable.status]) {
//                    VehicleStatus.ATIVO -> VehicleReport.Distributions.Status.ATIVO
//                    VehicleStatus.MANUTENCAO -> VehicleReport.Distributions.Status.MANUTENCAO
//                    VehicleStatus.INATIVO -> VehicleReport.Distributions.Status.INATIVO
//                },
//                count = it[VehiclesTable.status.count()]
//            ) }
//
//        // --- Query única agregando viagens, top driver e despesas ---
//        val fuelCostExpr = ExpensesTable.amount.sum().alias("fuel_cost")
//
//        val aggregated = (VehiclesTable leftJoin TripsTable leftJoin DriversTable leftJoin ExpensesTable)
//            .select(
//                VehiclesTable.id,
//                VehiclesTable.plate,
//                TripsTable.driverId,
//                DriversTable.name,
//                TripsTable.id.count(),
//                TripsTable.distanceKm.sum(),
//                fuelCostExpr,           // soma de combustível
//                ExpensesTable.date.max(),
//                TripsTable.status.count()
//            )
//            .where {
//                VehiclesTable.deletedAt.isNull() and
//                        (TripsTable.startTime.between(startDateTime, endDateTime) or TripsTable.id.isNull())
//            }
//            .groupBy(
//                VehiclesTable.id,
//                VehiclesTable.plate,
//                TripsTable.driverId,
//                DriversTable.name
//            )
//            .map { row ->
//                val plate = row[VehiclesTable.plate]
//                val totalTrips = row[TripsTable.id.count()].toLong()
//                val totalKm = row[TripsTable.distanceKm.sum()]?.toInt() ?: 0
//                val topDriver = VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver(
//                    name = row[DriversTable.name],
//                    trips = totalTrips.toInt()
//                )
//                val fuelCost = row[fuelCostExpr]?.toDouble() ?: 0.0
//                val totalCost = row[ExpensesTable.amount.sum()]?.toDouble() ?: 0.0
//                val lastMaintenanceDate = row[ExpensesTable.date.max()]?.toString()
//                val tripsInProgress = row[TripsTable.status.count()] ?: 0L
//                val isInUse = tripsInProgress > 0
//
//                VehicleReport.UsageStats.TotalDistanceByVehicle(
//                    plate = plate,
//                    totalKm = totalKm,
//                    totalTrips = totalTrips,
//                    topDriver = topDriver,
//                    fuelCost = fuelCost,
//                    maintenanceCost = totalCost,
//                    totalCost = totalCost + fuelCost,
//                    lastMaintenanceDate = lastMaintenanceDate,
//                    isInUse = isInUse
//                )
//            }
//
//
//        // --- Consumo de combustível por veículo ---
//        val fuelConsumptionByVehicle = aggregated.map {
//            VehicleReport.UsageStats.FuelConsumptionByVehicle(
//                plate = it.plate,
//                litersPerKm = if (it.totalKm > 0) it.fuelCost / it.totalKm else 0.0
//            )
//        }
//
//        ServiceResponse(
//            status = HttpStatusCode.OK,
//            data = VehicleReport(
//                distributions = VehicleReport.Distributions(
//                    byBrand = byBrand,
//                    byYear = byYear,
//                    byStatus = byStatus
//                ),
//                usageStats = VehicleReport.UsageStats(
//                    totalDistanceByVehicle = aggregated,
//                    fuelConsumptionByVehicle = fuelConsumptionByVehicle
//                )
//            )
//        )
//    }
//    ⏱ Vehicle report service execution time: 298ms


//    suspend fun getReport(
//        startDate: LocalDate? = null,
//        endDate: LocalDate? = null
//    ): ServiceResponse<VehicleReport> = DatabaseFactory.dbQuery {
//
//        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//        val start = startDate ?: LocalDate(now.year, now.month, 1)
//        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
//
//        val timeZone = TimeZone.currentSystemDefault()
//        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//
//        // --- Distribuições ---
//        val byBrand = VehiclesTable
//            .select(VehiclesTable.brand, VehiclesTable.brand.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.brand)
//            .map { VehicleReport.Distributions.ByBrand(it[VehiclesTable.brand] ?: "Desconhecida", it[VehiclesTable.brand.count()]) }
//
//        val byYear = VehiclesTable
//            .select(VehiclesTable.year, VehiclesTable.year.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.year)
//            .map { VehicleReport.Distributions.ByYear(it[VehiclesTable.year] ?: 0, it[VehiclesTable.year.count()]) }
//
//        val byStatus = VehiclesTable
//            .select(VehiclesTable.status, VehiclesTable.status.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.status)
//            .map { VehicleReport.Distributions.ByStatus(
//                status = when(it[VehiclesTable.status]) {
//                    VehicleStatus.ATIVO -> VehicleReport.Distributions.Status.ATIVO
//                    VehicleStatus.MANUTENCAO -> VehicleReport.Distributions.Status.MANUTENCAO
//                    VehicleStatus.INATIVO -> VehicleReport.Distributions.Status.INATIVO
//                },
//                count = it[VehiclesTable.status.count()]
//            ) }
//
//        val vehicles = VehiclesTable.selectAll().where { VehiclesTable.deletedAt.isNull() }.toList()
//
//        val vehicleIds: List<Int> = vehicles.map { it[VehiclesTable.id] }
//
//        val tripsAgg: Map<Int, Triple<Double, Long, VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver?>> =
//            TripsTable
//                .select(
//                    TripsTable.vehicleId,
//                    TripsTable.driverId,
//                    TripsTable.distanceKm.sum(),
//                    TripsTable.id.count()
//                )
//                .where {
//                    TripsTable.vehicleId.inList(vehicleIds) and
//                            TripsTable.startTime.between(startDateTime, endDateTime)
//                }
//                .groupBy(TripsTable.vehicleId, TripsTable.driverId)
//                .map { row: ResultRow ->
//                    val vehicleId = row[TripsTable.vehicleId]
//                    val driverId = row[TripsTable.driverId]
//                    val totalKm = row[TripsTable.distanceKm.sum()] ?: 0.0
//                    val totalTrips = row[TripsTable.id.count()].toLong()
//                    vehicleId to (driverId to Pair(totalKm, totalTrips))
//                }
//                .groupBy({ it.first }, { it.second }) // Agrupa por veículo
//                .mapValues { entry ->
//                    val top = entry.value.maxByOrNull { it.second.second }  // driver com mais trips
//                    if (top != null) {
//                        val (driverId, data) = top
//                        val driverName: String? = DriversTable
//                            .selectAll().where { DriversTable.id eq driverId }
//                            .firstOrNull()?.get(DriversTable.name)
//                        val topDriver = VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver(
//                            name = driverName,
//                            trips = data.second.toInt()
//                        )
//                        val totalKm: Double = entry.value.sumOf { it.second.first.toDouble() }
//                        val totalTrips = entry.value.sumOf { it.second.second }
//                        Triple(totalKm, totalTrips, topDriver)
//                    } else {
//                        Triple(0.0, 0L, null)
//                    }
//                }
//
//
//        // --- Aggregação de despesas por veículo ---
//        val expensesAgg = ExpensesTable
//            .select(
//                ExpensesTable.vehicleId,
//                ExpensesTable.amount.sum(),
//                ExpensesTable.liters.sum(),
//                ExpensesTable.date.max()
//            )
//            .where { ExpensesTable.vehicleId.inList(vehicles.map { it[VehiclesTable.id] }) }
//            .groupBy(ExpensesTable.vehicleId)
//            .associate { row ->
//                val vehicleId = row[ExpensesTable.vehicleId]
//                val totalCost = row[ExpensesTable.amount.sum()]?.toDouble() ?: 0.0
//                val totalLiters = row[ExpensesTable.liters.sum()]?.toDouble() ?: 0.0
//                val lastMaintenance = row[ExpensesTable.date.max()]?.toString()
//                vehicleId to Triple(totalCost, totalLiters, lastMaintenance)
//            }
//
//        val totalDistanceByVehicle = vehicles.map { vehicle ->
//            val vehicleId = vehicle[VehiclesTable.id]
//            val plate = vehicle[VehiclesTable.plate]
//
//            val tripsData = tripsAgg[vehicleId]
//            val (totalKm, totalTrips, topDriver) = tripsData ?: Triple(0.0, 0L, null)
//
//            val expenseData = expensesAgg[vehicleId]
//            val (totalCost, totalLiters, lastMaintenanceDate) = expenseData ?: Triple(0.0, 0.0, null)
//
//            VehicleReport.UsageStats.TotalDistanceByVehicle(
//                plate = plate,
//                totalKm = totalKm.toInt(),
//                totalTrips = totalTrips,
//                topDriver = topDriver,
//                fuelCost = 0.0, // Se quiser, filtrar apenas combustível no SQL
//                maintenanceCost = totalCost,
//                totalCost = totalCost,
//                lastMaintenanceDate = lastMaintenanceDate,
//                isInUse = TripsTable.selectAll()
//                    .where { TripsTable.vehicleId eq vehicleId and (TripsTable.status eq TripStatus.EM_ANDAMENTO) }.any()
//            )
//        }
//
//        val fuelConsumptionByVehicle = totalDistanceByVehicle.map {
//            VehicleReport.UsageStats.FuelConsumptionByVehicle(
//                plate = it.plate,
//                litersPerKm = if (it.totalKm > 0) it.fuelCost / it.totalKm else 0.0
//            )
//        }
//
//        ServiceResponse(
//            status = HttpStatusCode.OK,
//            data = VehicleReport(
//                distributions = VehicleReport.Distributions(
//                    byBrand = byBrand,
//                    byYear = byYear,
//                    byStatus = byStatus
//                ),
//                usageStats = VehicleReport.UsageStats(
//                    totalDistanceByVehicle = totalDistanceByVehicle,
//                    fuelConsumptionByVehicle = fuelConsumptionByVehicle
//                )
//            )
//        )
//    }
    //tempo de execucao = 361ms

//    suspend fun getReport(
//        startDate: LocalDate? = null,
//        endDate: LocalDate? = null
//    ): ServiceResponse<VehicleReport> = DatabaseFactory.dbQuery {
//
//        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//        val start = startDate ?: LocalDate(now.year, now.month, 1)
//        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
//        val timeZone = TimeZone.currentSystemDefault()
//        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//
//        // --- Distribuições ---
//        val byBrand = VehiclesTable
//            .select(VehiclesTable.brand, VehiclesTable.brand.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.brand)
//            .map { VehicleReport.Distributions.ByBrand(it[VehiclesTable.brand] ?: "Desconhecida", it[VehiclesTable.brand.count()]) }
//
//        val byYear = VehiclesTable
//            .select(VehiclesTable.year, VehiclesTable.year.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.year)
//            .map { VehicleReport.Distributions.ByYear(it[VehiclesTable.year] ?: 0, it[VehiclesTable.year.count()]) }
//
//        val byStatus = VehiclesTable
//            .select(VehiclesTable.status, VehiclesTable.status.count())
//            .where { VehiclesTable.deletedAt.isNull() }
//            .groupBy(VehiclesTable.status)
//            .map { VehicleReport.Distributions.ByStatus(
//                status = when(it[VehiclesTable.status]) {
//                    VehicleStatus.ATIVO -> VehicleReport.Distributions.Status.ATIVO
//                    VehicleStatus.MANUTENCAO -> VehicleReport.Distributions.Status.MANUTENCAO
//                    VehicleStatus.INATIVO -> VehicleReport.Distributions.Status.INATIVO
//                },
//                count = it[VehiclesTable.status.count()]
//            ) }
//
//        // --- Query única agregando viagens, top driver, manutenção e combustível ---
//        val aggregated = (VehiclesTable leftJoin TripsTable leftJoin DriversTable leftJoin ExpensesTable)
//            .select(
//                VehiclesTable.id,
//                VehiclesTable.plate,
//                TripsTable.driverId,
//                DriversTable.name,
//                TripsTable.id.count(),          // totalTrips
//                TripsTable.distanceKm.sum(),    // totalKm
//                ExpensesTable.amount.sum(),     // totalCost (pode filtrar tipo)
//                ExpensesTable.date.max(),       // lastMaintenanceDate
//                TripsTable.status.count()       // para isInUse
//            )
//            .where {
//                VehiclesTable.deletedAt.isNull() and
//                        (TripsTable.startTime.between(startDateTime, endDateTime) or TripsTable.id.isNull())
//            }
//            .groupBy(VehiclesTable.id, TripsTable.driverId, DriversTable.name)
//            .map { row ->
//                val plate = row[VehiclesTable.plate]
//                val totalTrips = row[TripsTable.id.count()].toLong()
//                val totalKm = row[TripsTable.distanceKm.sum()]?.toInt() ?: 0
//                val topDriver = VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver(
//                    name = row[DriversTable.name],
//                    trips = totalTrips.toInt()
//                )
//                val totalCost = row[ExpensesTable.amount.sum()]?.toDouble() ?: 0.0
//                val lastMaintenanceDate = row[ExpensesTable.date.max()]?.toString()
//                val tripsInProgress = row[TripsTable.status.count()] ?: 0L
//                val isInUse = tripsInProgress > 0
//
//                VehicleReport.UsageStats.TotalDistanceByVehicle(
//                    plate = plate,
//                    totalKm = totalKm,
//                    totalTrips = totalTrips,
//                    topDriver = topDriver,
//                    fuelCost = 0.0,       // se quiser, filtrar despesas do tipo combustível
//                    maintenanceCost = totalCost,
//                    totalCost = totalCost,
//                    lastMaintenanceDate = lastMaintenanceDate,
//                    isInUse = isInUse
//                )
//            }
//
//        val fuelConsumptionByVehicle = aggregated.map {
//            VehicleReport.UsageStats.FuelConsumptionByVehicle(
//                plate = it.plate,
//                litersPerKm = if (it.totalKm > 0) it.fuelCost / it.totalKm else 0.0
//            )
//        }
//
//        ServiceResponse(
//            status = HttpStatusCode.OK,
//            data = VehicleReport(
//                distributions = VehicleReport.Distributions(
//                    byBrand = byBrand,
//                    byYear = byYear,
//                    byStatus = byStatus
//                ),
//                usageStats = VehicleReport.UsageStats(
//                    totalDistanceByVehicle = aggregated,
//                    fuelConsumptionByVehicle = fuelConsumptionByVehicle
//                )
//            )
//        )
//    }
    // tempo de execucao = 233ms

//    suspend fun getReport(
//        startDate: LocalDate? = null,
//        endDate: LocalDate? = null
//    ): ServiceResponse<VehicleReport> {
//        return DatabaseFactory.dbQuery {
//            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//            val start = startDate ?: LocalDate(now.year, now.month, 1)
//            val daysInMonth = if (now.month == kotlinx.datetime.Month.FEBRUARY &&
//                now.year % 4 == 0 && (now.year % 100 != 0 || now.year % 400 == 0)) {
//                29
//            } else {
//                now.month.maxLength()
//            }
//            val end = endDate ?: LocalDate(now.year, now.month, daysInMonth)
//
//            // --- Distribuições ---
//            val byBrand = VehiclesTable
//                .select(VehiclesTable.brand, VehiclesTable.brand.count())
//                .where { VehiclesTable.deletedAt.isNull() }
//                .groupBy(VehiclesTable.brand)
//                .map {
//                    VehicleReport.Distributions.ByBrand(
//                        brand = it[VehiclesTable.brand] ?: "Desconhecida",
//                        count = it[VehiclesTable.brand.count()]
//                    )
//                }
//
//            val byYear = VehiclesTable
//                .select(VehiclesTable.year, VehiclesTable.year.count())
//                .where { VehiclesTable.deletedAt.isNull() }
//                .groupBy(VehiclesTable.year)
//                .map {
//                    VehicleReport.Distributions.ByYear(
//                        year = it[VehiclesTable.year] ?: 0,
//                        count = it[VehiclesTable.year.count()]
//                    )
//                }
//
//            val byStatus = VehiclesTable
//                .select(VehiclesTable.status, VehiclesTable.status.count())
//                .where { VehiclesTable.deletedAt.isNull() }
//                .groupBy(VehiclesTable.status)
//                .map {
//                    VehicleReport.Distributions.ByStatus(
//                        status = when (it[VehiclesTable.status]) {
//                            VehicleStatus.ATIVO -> VehicleReport.Distributions.Status.ATIVO
//                            VehicleStatus.MANUTENCAO -> VehicleReport.Distributions.Status.MANUTENCAO
//                            VehicleStatus.INATIVO -> VehicleReport.Distributions.Status.INATIVO
//                        },
//                        count = it[VehiclesTable.status.count()]
//                    )
//                }
//
//            // --- Estatísticas de uso por veículo ---
//            val totalDistanceByVehicle = VehiclesTable
//                .selectAll()
//                .where { VehiclesTable.deletedAt.isNull<LocalDateTime?>() }
//                .map { vehicleRow ->
//
//                    val vehicleId = vehicleRow[VehiclesTable.id]
//                    val plate = vehicleRow[VehiclesTable.plate]
//                    val timeZone = TimeZone.currentSystemDefault()
//                    val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
//                    val endDateTime = end.plus(DatePeriod(days = 1))
//                        .atStartOfDayIn(timeZone)
//                        .toLocalDateTime(timeZone)
//
//                    // --- Viagens do veículo no período ---
//                    val trips = TripsTable
//                        .selectAll()
//                        .where {
//                            (TripsTable.vehicleId eq vehicleId) and
//                                    (TripsTable.startTime.between(startDateTime, endDateTime))
//                        }
//
//                    val totalKm = trips.sumOf { it[TripsTable.distanceKm]?.toDouble() ?: 0.0 }
//                    val totalTrips = trips.count().toLong()
//
//                    // --- Top driver ---
//                    val driverTripCount = trips.groupingBy { it[TripsTable.driverId] }.eachCount()
//                    val topDriverEntry = driverTripCount.maxByOrNull { it.value }
//                    val topDriver = topDriverEntry?.let { (driverId, tripsCount) ->
//                        val driverName = DriversTable
//                            .selectAll().where { DriversTable.id eq driverId }
//                            .firstOrNull()?.get(DriversTable.name)
//                        VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver(
//                            name = driverName,
//                            trips = tripsCount
//                        )
//                    }
//
//                    // --- Combustível ---
//                    val fuelExpenses = ExpensesTable
//                        .selectAll()
//                        .where {
//                            (ExpensesTable.vehicleId eq vehicleId) and
//                                    (ExpensesTable.type like "%Combustível%") and
//                                    (ExpensesTable.date.between(start, end))
//                        }
//                    val fuelCost = fuelExpenses.sumOf { it[ExpensesTable.amount].toDouble() }
//                    val totalLiters = fuelExpenses.sumOf { it[ExpensesTable.liters]?.toDouble() ?: 0.0 }
//
//                    // --- Manutenção ---
//                    val maintenanceCost = ExpensesTable
//                        .selectAll()
//                        .where {
//                            (ExpensesTable.vehicleId eq vehicleId) and
//                                    (ExpensesTable.type like "%Manutenção%") and
//                                    (ExpensesTable.date.between(start, end))
//                        }.sumOf { it[ExpensesTable.amount].toDouble() }
//
//                    val lastMaintenanceDate = ExpensesTable
//                        .select(ExpensesTable.date.max())
//                        .where {
//                            (ExpensesTable.vehicleId eq vehicleId) and
//                                    (ExpensesTable.type like "%Manutenção%")
//                        }
//                        .firstOrNull()
//                        ?.get(ExpensesTable.date.max())
//                        ?.toString() // ou formatar para padrão desejado
//
//
//                    VehicleReport.UsageStats.TotalDistanceByVehicle(
//                        plate = plate,
//                        totalKm = totalKm.toInt(),
//                        totalTrips = totalTrips,
//                        topDriver = topDriver,
//                        fuelCost = fuelCost,
//                        maintenanceCost = maintenanceCost,
//                        totalCost = fuelCost + maintenanceCost,
//                        lastMaintenanceDate = lastMaintenanceDate,
//                        isInUse = trips.any { it[TripsTable.status] == TripStatus.EM_ANDAMENTO }
//                    )
//                }
//
//            // --- Consumo de combustível ---
//            val fuelConsumptionByVehicle = totalDistanceByVehicle.map {
//                VehicleReport.UsageStats.FuelConsumptionByVehicle(
//                    plate = it.plate,
//                    litersPerKm = if (it.totalKm > 0) it.fuelCost / it.totalKm else 0.0
//                )
//            }
//
//            ServiceResponse(
//                status = HttpStatusCode.OK,
//                data = VehicleReport(
//                    distributions = VehicleReport.Distributions(
//                        byBrand = byBrand,
//                        byYear = byYear,
//                        byStatus = byStatus
//                    ),
//                    usageStats = VehicleReport.UsageStats(
//                        totalDistanceByVehicle = totalDistanceByVehicle,
//                        fuelConsumptionByVehicle = fuelConsumptionByVehicle
//                    )
//                )
//            )
//        }
//    }
//    ⏱ Vehicle report service execution time: 354ms


    suspend fun softDeleteVehicle(id: Int): ServiceResponse<Message> {
        val existingVehicle = DatabaseFactory.dbQuery {
            VehiclesTable
                .selectAll()
                .where { VehiclesTable.id eq id and VehiclesTable.deletedAt.isNull() }
                .singleOrNull()
        }

        if (existingVehicle == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Veículo não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            VehiclesTable.update({ VehiclesTable.id eq id }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Veículo removido com sucesso")
        )
    }
}
