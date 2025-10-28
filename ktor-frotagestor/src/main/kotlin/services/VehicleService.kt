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
import kotlinx.datetime.Month
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
                .where { VehiclesTable.id eq id and (VehiclesTable.status neq VehicleStatus.INATIVO) }
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
            VehiclesTable.selectAll().where { VehiclesTable.id eq id and (VehiclesTable.status neq VehicleStatus.INATIVO) }
                .singleOrNull()?.let {
                    Vehicle(
                        id = it[VehiclesTable.id],
                        plate = it[VehiclesTable.plate],
                        model = it[VehiclesTable.model],
                        brand = it[VehiclesTable.brand],
                        year = it[VehiclesTable.year],
                        status = it[VehiclesTable.status],
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
            val activeCount = VehiclesTable.selectAll().where { VehiclesTable.status eq VehicleStatus.ATIVO }.count()
            val maintenanceCount = VehiclesTable.selectAll()
                .where { VehiclesTable.status eq VehicleStatus.MANUTENCAO and VehiclesTable.deletedAt.isNull() }
                .count()

            val lastVehicleRow = VehiclesTable
                .selectAll().where { VehiclesTable.deletedAt.isNull() }
                .orderBy(VehiclesTable.id, SortOrder.DESC)
                .limit(1).singleOrNull()

            val lastVehicle = lastVehicleRow?.let {
                Vehicle(
                    id = it[VehiclesTable.id],
                    plate = it[VehiclesTable.plate],
                    model = it[VehiclesTable.model],
                    brand = it[VehiclesTable.brand],
                    year = it[VehiclesTable.year],
                    status = it[VehiclesTable.status],
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

    suspend fun getTripsByVehicle(
        vehicleId: Int,
        startDate: LocalDate?,
        endDate: LocalDate?,
        page: Int = 1,
        limit: Int = 10
    ): ServiceResponse<PaginatedResponse<Trip>> {
        return DatabaseFactory.dbQuery {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            // Calcula o primeiro e último dia do mês atual manualmente
            val firstDayOfMonth = LocalDate(today.year, today.month, 1)
            val lastDayOfMonth = LocalDate(
                today.year,
                today.month,
                when (today.month) {
                    Month.FEBRUARY -> if (today.year % 4 == 0 && (today.year % 100 != 0 || today.year % 400 == 0)) 29 else 28
                    Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
                    else -> 31
                }
            )

            // Se não vierem as datas, usa o mês atual
            val effectiveStart = startDate ?: firstDayOfMonth
            val effectiveEnd = endDate ?: lastDayOfMonth

            val startDateTime = effectiveStart.atStartOfDayIn(TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val endDateTime = effectiveEnd.plus(DatePeriod(days = 1))
                .atStartOfDayIn(TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val query = TripsTable
                .join(DriversTable, JoinType.INNER) { TripsTable.driverId eq DriversTable.id }
                .join(VehiclesTable, JoinType.INNER) { TripsTable.vehicleId eq VehiclesTable.id }
                .selectAll()
                .where {
                    (TripsTable.vehicleId eq vehicleId) and
                            (TripsTable.startTime greaterEq startDateTime) and
                            (TripsTable.startTime less endDateTime) and
                            (VehiclesTable.deletedAt.isNull())
                }

            val total = query.count()
            val results = query
                .orderBy(TripsTable.startTime to SortOrder.ASC)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Trip(
                        id = it[TripsTable.id],
                        vehicleId = it[TripsTable.vehicleId],
                        driverId = it[TripsTable.driverId],
                        startLocation = it[TripsTable.startLocation],
                        endLocation = it[TripsTable.endLocation],
                        startTime = it[TripsTable.startTime],
                        endTime = it[TripsTable.endTime],
                        distanceKm = it[TripsTable.distanceKm]?.toDouble(),
                        status = it[TripsTable.status],
                        driverName = it[DriversTable.name],
                        vehiclePlate = it[VehiclesTable.plate]
                    )
                }

            ServiceResponse(
                status = if (results.isEmpty()) HttpStatusCode.NotFound else HttpStatusCode.OK,
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

    suspend fun getExpensesByVehicle(
        vehicleId: Int,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        page: Int = 1,
        limit: Int = 10
    ): ServiceResponse<PaginatedResponse<Expense>> {
        return DatabaseFactory.dbQuery {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val firstDayOfMonth = LocalDate(today.year, today.month, 1)
            val lastDayOfMonth = LocalDate(
                today.year,
                today.month,
                when (today.month) {
                    Month.FEBRUARY -> if (today.year % 4 == 0 && (today.year % 100 != 0 || today.year % 400 == 0)) 29 else 28
                    Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
                    else -> 31
                }
            )
            val effectiveStart = startDate ?: firstDayOfMonth
            val effectiveEnd = endDate ?: lastDayOfMonth
            val query = ExpensesTable
                .join(VehiclesTable, JoinType.INNER) { ExpensesTable.vehicleId eq VehiclesTable.id }
                .join(DriversTable, JoinType.LEFT) { ExpensesTable.driverId eq DriversTable.id }
                .select(
                    ExpensesTable.id,
                    ExpensesTable.vehicleId,
                    ExpensesTable.driverId,
                    ExpensesTable.tripId,
                    ExpensesTable.date,
                    ExpensesTable.type,
                    ExpensesTable.amount,
                    ExpensesTable.description,
                    ExpensesTable.liters,
                    ExpensesTable.pricePerLiter,
                    ExpensesTable.odometer,
                    VehiclesTable.plate,
                    DriversTable.name
                )
                .where { (ExpensesTable.vehicleId eq vehicleId) and (ExpensesTable.date greaterEq effectiveStart) and (ExpensesTable.date lessEq effectiveEnd) }

            val total = query.count()
            val results = query
                .orderBy(ExpensesTable.date to SortOrder.ASC)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Expense(
                        id = it[ExpensesTable.id],
                        vehicleId = it[ExpensesTable.vehicleId],
                        driverId = it[ExpensesTable.driverId],
                        tripId = it[ExpensesTable.tripId],
                        date = it[ExpensesTable.date],
                        type = it[ExpensesTable.type],
                        amount = it[ExpensesTable.amount].toDouble(),
                        description = it[ExpensesTable.description],
                        liters = it[ExpensesTable.liters]?.toDouble(),
                        pricePerLiter = it[ExpensesTable.pricePerLiter]?.toDouble(),
                        odometer = it[ExpensesTable.odometer],
                        driverName = it[DriversTable.name],
                        vehiclePlate = it[VehiclesTable.plate]
                    )
                }

            ServiceResponse(
                status = if (results.isEmpty()) HttpStatusCode.NotFound else HttpStatusCode.OK,
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

    suspend fun getTopDriverByVehicle(
        vehicleId: Int,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): ServiceResponse<Any> {
        return DatabaseFactory.dbQuery {
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                val firstDayOfMonth = LocalDate(today.year, today.month, 1)
                val lastDayOfMonth = LocalDate(
                    today.year,
                    today.month,
                    when (today.month) {
                        Month.FEBRUARY -> if (today.year % 4 == 0 && (today.year % 100 != 0 || today.year % 400 == 0)) 29 else 28
                        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
                        else -> 31
                    }
                )

                // Usa as datas informadas ou o mês atual como padrão
                val effectiveStart = startDate ?: firstDayOfMonth
                val effectiveEnd = endDate ?: lastDayOfMonth

                val startDateTime = effectiveStart.atStartOfDayIn(TimeZone.currentSystemDefault())
                val endDateTime = effectiveEnd.plus(DatePeriod(days = 1))
                    .atStartOfDayIn(TimeZone.currentSystemDefault())

                // Query: motorista que mais usou o veículo no período
                val result = (TripsTable innerJoin DriversTable)
                    .select(
                        DriversTable.id,
                        DriversTable.name,
                        DriversTable.cpf,
                        DriversTable.cnh,
                        DriversTable.cnhCategory,
                        DriversTable.cnhExpiration,
                        DriversTable.phone,
                        DriversTable.email,
                        DriversTable.status,
                        DriversTable.deletedAt,
                        TripsTable.id.count()
                    )
                    .where {
                        (TripsTable.vehicleId eq vehicleId) and
                                (TripsTable.startTime greaterEq startDateTime.toLocalDateTime(TimeZone.currentSystemDefault())) and
                                (TripsTable.startTime less endDateTime.toLocalDateTime(TimeZone.currentSystemDefault()))
                    }
                    .groupBy(
                        DriversTable.id,
                        DriversTable.name,
                        DriversTable.cpf,
                        DriversTable.cnh,
                        DriversTable.cnhCategory,
                        DriversTable.cnhExpiration,
                        DriversTable.phone,
                        DriversTable.email,
                        DriversTable.status,
                        DriversTable.deletedAt
                    )
                    .orderBy(TripsTable.id.count(), SortOrder.DESC)
                    .limit(1)
                    .map {
                        Driver(
                            id = it[DriversTable.id],
                            name = it[DriversTable.name],
                            cpf = it[DriversTable.cpf],
                            cnh = it[DriversTable.cnh],
                            cnhCategory = it[DriversTable.cnhCategory],
                            cnhExpiration = it[DriversTable.cnhExpiration],
                            phone = it[DriversTable.phone],
                            email = it[DriversTable.email],
                            status = it[DriversTable.status],
                            deletedAt = it[DriversTable.deletedAt]
                        )
                    }
                    .firstOrNull()

                ServiceResponse(
                    status = HttpStatusCode.OK,
                    data = result ?: mapOf(
                        "message" to "Nenhum motorista encontrado para este veículo no período ou veículo não existe"
                    )
                )
            } catch (e: Exception) {
                println("Error in getTopDriverByVehicle: ${e.message}")
                ServiceResponse(
                    status = HttpStatusCode.InternalServerError,
                    data = mapOf("message" to "Erro interno")
                )
            }
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
