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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.selectAll

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
    ): ServiceResponse<VehicleReport> {
        return DatabaseFactory.dbQuery {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val start = startDate ?: LocalDate(now.year, now.month, 1)
            val daysInMonth = if (now.month == kotlinx.datetime.Month.FEBRUARY &&
                now.year % 4 == 0 && (now.year % 100 != 0 || now.year % 400 == 0)) {
                29
            } else {
                now.month.maxLength()
            }
            val end = endDate ?: LocalDate(now.year, now.month, daysInMonth)

            // --- Distribuições ---
            val byBrand = VehiclesTable
                .select(VehiclesTable.brand, VehiclesTable.brand.count())
                .where { VehiclesTable.deletedAt.isNull() }
                .groupBy(VehiclesTable.brand)
                .map {
                    VehicleReport.Distributions.ByBrand(
                        brand = it[VehiclesTable.brand] ?: "Desconhecida",
                        count = it[VehiclesTable.brand.count()]
                    )
                }

            val byYear = VehiclesTable
                .select(VehiclesTable.year, VehiclesTable.year.count())
                .where { VehiclesTable.deletedAt.isNull() }
                .groupBy(VehiclesTable.year)
                .map {
                    VehicleReport.Distributions.ByYear(
                        year = it[VehiclesTable.year] ?: 0,
                        count = it[VehiclesTable.year.count()]
                    )
                }

            val byStatus = VehiclesTable
                .select(VehiclesTable.status, VehiclesTable.status.count())
                .where { VehiclesTable.deletedAt.isNull() }
                .groupBy(VehiclesTable.status)
                .map {
                    VehicleReport.Distributions.ByStatus(
                        status = when (it[VehiclesTable.status]) {
                            VehicleStatus.ATIVO -> VehicleReport.Distributions.Status.ATIVO
                            VehicleStatus.MANUTENCAO -> VehicleReport.Distributions.Status.MANUTENCAO
                            VehicleStatus.INATIVO -> VehicleReport.Distributions.Status.INATIVO
                        },
                        count = it[VehiclesTable.status.count()]
                    )
                }

            // --- Estatísticas de uso por veículo ---
            val totalDistanceByVehicle = VehiclesTable
                .selectAll()
                .where { VehiclesTable.deletedAt.isNull<LocalDateTime?>() }
                .map { vehicleRow ->

                    val vehicleId = vehicleRow[VehiclesTable.id]
                    val plate = vehicleRow[VehiclesTable.plate]
                    val timeZone = TimeZone.currentSystemDefault()
                    val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
                    val endDateTime = end.plus(DatePeriod(days = 1))
                        .atStartOfDayIn(timeZone)
                        .toLocalDateTime(timeZone)

                    // --- Viagens do veículo no período ---
                    val trips = TripsTable
                        .selectAll()
                        .where {
                            (TripsTable.vehicleId eq vehicleId) and
                                    (TripsTable.startTime.between(startDateTime, endDateTime))
                        }

                    val totalKm = trips.sumOf { it[TripsTable.distanceKm]?.toDouble() ?: 0.0 }
                    val totalTrips = trips.count().toLong()

                    // --- Top driver ---
                    val driverTripCount = trips.groupingBy { it[TripsTable.driverId] }.eachCount()
                    val topDriverEntry = driverTripCount.maxByOrNull { it.value }
                    val topDriver = topDriverEntry?.let { (driverId, tripsCount) ->
                        val driverName = DriversTable
                            .selectAll().where { DriversTable.id eq driverId }
                            .firstOrNull()?.get(DriversTable.name)
                        VehicleReport.UsageStats.TotalDistanceByVehicle.TopDriver(
                            name = driverName,
                            trips = tripsCount
                        )
                    }

                    // --- Combustível ---
                    val fuelExpenses = ExpensesTable
                        .selectAll()
                        .where {
                            (ExpensesTable.vehicleId eq vehicleId) and
                                    (ExpensesTable.type like "%Combustível%") and
                                    (ExpensesTable.date.between(start, end))
                        }
                    val fuelCost = fuelExpenses.sumOf { it[ExpensesTable.amount].toDouble() }
                    val totalLiters = fuelExpenses.sumOf { it[ExpensesTable.liters]?.toDouble() ?: 0.0 }

                    // --- Manutenção ---
                    val maintenanceCost = ExpensesTable
                        .selectAll()
                        .where {
                            (ExpensesTable.vehicleId eq vehicleId) and
                                    (ExpensesTable.type like "%Manutenção%") and
                                    (ExpensesTable.date.between(start, end))
                        }.sumOf { it[ExpensesTable.amount].toDouble() }

                    val lastMaintenanceDate = ExpensesTable
                        .select(ExpensesTable.date.max())
                        .where {
                            (ExpensesTable.vehicleId eq vehicleId) and
                                    (ExpensesTable.type like "%Manutenção%")
                        }
                        .firstOrNull()
                        ?.get(ExpensesTable.date.max())
                        ?.toString() // ou formatar para padrão desejado


                    VehicleReport.UsageStats.TotalDistanceByVehicle(
                        plate = plate,
                        totalKm = totalKm.toInt(),
                        totalTrips = totalTrips,
                        topDriver = topDriver,
                        fuelCost = fuelCost,
                        maintenanceCost = maintenanceCost,
                        totalCost = fuelCost + maintenanceCost,
                        lastMaintenanceDate = lastMaintenanceDate,
                        isInUse = trips.any { it[TripsTable.status] == TripStatus.EM_ANDAMENTO }
                    )
                }

            // --- Consumo de combustível ---
            val fuelConsumptionByVehicle = totalDistanceByVehicle.map {
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
                        totalDistanceByVehicle = totalDistanceByVehicle,
                        fuelConsumptionByVehicle = fuelConsumptionByVehicle
                    )
                )
            )
        }
    }


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
