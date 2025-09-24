package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateVehicle
import com.frotagestor.validations.validatePartialVehicle
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

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
                        status = it[VehiclesTable.status]
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
                        status = it[VehiclesTable.status]
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
