package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.DriversTable
import com.frotagestor.interfaces.Driver
import com.frotagestor.interfaces.DriverStatus
import com.frotagestor.interfaces.Message
import com.frotagestor.interfaces.PaginatedResponse
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateDriver
import com.frotagestor.validations.validatePartialDriver
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import kotlin.let
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class DriverService {

    suspend fun createDriver(req: String): ServiceResponse<Message> {
        val newDriver = validateDriver(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingDriver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.cpf eq newDriver.cpf }
                .singleOrNull()
        }

        if (existingDriver != null) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Motorista já registrado!")
            )
        }

        DatabaseFactory.dbQuery {
            DriversTable.insert {
                it[name] = newDriver.name
                it[cpf] = newDriver.cpf
                it[cnh] = newDriver.cnh
                it[cnhCategory] = newDriver.cnhCategory
                it[cnhExpiration] = newDriver.cnhExpiration
                it[phone] = newDriver.phone
                it[email] = newDriver.email
                it[status] = newDriver.status
                it[deletedAt] = null
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Motorista criado com sucesso")
        )
    }

    suspend fun updateDriver(id: Int, req: String): ServiceResponse<Message> {
        val updatedDriver = validatePartialDriver(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingDriver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.id eq id and DriversTable.deletedAt.isNull() }
                .singleOrNull()
        }

        if (existingDriver == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Motorista não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            DriversTable.update({ DriversTable.id eq id }) {
                updatedDriver.name?.let { n -> it[name] = n }
                updatedDriver.cpf?.let { c -> it[cpf] = c }
                updatedDriver.cnh?.let { c -> it[cnh] = c }
                updatedDriver.cnhCategory?.let { cat -> it[cnhCategory] = cat }
                updatedDriver.cnhExpiration?.let { exp -> it[cnhExpiration] = exp }
                updatedDriver.phone?.let { p -> it[phone] = p }
                updatedDriver.email?.let { e -> it[email] = e }
                updatedDriver.status?.let { s -> it[status] = s }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Motorista atualizado com sucesso")
        )
    }

    suspend fun getAllDrivers(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = DriversTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        nameFilter: String? = null,
        cpfFilter: String? = null,
        cnhFilter: String? = null,
        cnhCategoryFilter: String? = null,
        cnhExpirationFilter: LocalDate? = null,
        phoneFilter: String? = null,
        emailFilter: String? = null,
        statusFilter: DriverStatus? = null
    ): ServiceResponse<PaginatedResponse<Driver>> {
        return DatabaseFactory.dbQuery {
            val query = DriversTable
                .selectAll()
                .apply {
                    if (statusFilter != DriverStatus.INATIVO) {
                        andWhere { DriversTable.deletedAt.isNull() }
                    }
                    if (idFilter != null) {
                        andWhere { DriversTable.id eq idFilter }
                    }
                    if (!nameFilter.isNullOrBlank()) {
                        andWhere { DriversTable.name like "%$nameFilter%" }
                    }
                    if (!cpfFilter.isNullOrBlank()) {
                        andWhere { DriversTable.cpf like "%$cpfFilter%" }
                    }
                    if (!cnhFilter.isNullOrBlank()) {
                        andWhere { DriversTable.cnh like "%$cnhFilter%" }
                    }
                    if (!cnhCategoryFilter.isNullOrBlank()) {
                        andWhere { DriversTable.cnhCategory eq cnhCategoryFilter }
                    }
                    if (cnhExpirationFilter != null) {
                        andWhere { DriversTable.cnhExpiration eq cnhExpirationFilter }
                    }
                    if (!phoneFilter.isNullOrBlank()) {
                        andWhere { DriversTable.phone like "%$phoneFilter%" }
                    }
                    if (!emailFilter.isNullOrBlank()) {
                        andWhere { DriversTable.email like "%$emailFilter%" }
                    }
                    if (statusFilter != null) {
                        andWhere { DriversTable.status eq statusFilter }
                    }
                }

            val total = query.count()

            val orderExpr = if (sortBy == DriversTable.cnhCategory) {
                CustomFunction<String>("UPPER", TextColumnType(), DriversTable.cnhCategory)
            } else {
                sortBy
            }

            val results = query
                .orderBy(orderExpr to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
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



    suspend fun findDriverById(id: Int): ServiceResponse<Any> {
        val driver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.id eq id and DriversTable.deletedAt.isNull() }
                .singleOrNull()
                ?.let {
                    Driver(
                        id = it[DriversTable.id],
                        name = it[DriversTable.name],
                        cpf = it[DriversTable.cpf],
                        cnh = it[DriversTable.cnh],
                        cnhCategory = it[DriversTable.cnhCategory],
                        cnhExpiration = it[DriversTable.cnhExpiration],
                        phone = it[DriversTable.phone],
                        email = it[DriversTable.email],
                        status = it[DriversTable.status]
                    )
                }
        }

        return if (driver == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = mapOf("message" to "Motorista não encontrado")
            )
        } else {
            ServiceResponse(
                status = HttpStatusCode.OK,
                data = driver
            )
        }
    }

    suspend fun softDeleteDriver(id: Int): ServiceResponse<Message> {
        val existingDriver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.id eq id and DriversTable.deletedAt.isNull() }
                .singleOrNull()
        }

        if (existingDriver == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Motorista não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            DriversTable.update({ DriversTable.id eq id }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Motorista removido com sucesso")
        )
    }
}
