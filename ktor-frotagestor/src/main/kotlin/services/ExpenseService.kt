package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.ExpensesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateExpense
import com.frotagestor.validations.validatePartialExpense
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ExpenseService {

    suspend fun createExpense(req: String): ServiceResponse<Message> {
        val newExpense = validateExpense(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        DatabaseFactory.dbQuery {
            ExpensesTable.insert {
                it[vehicleId] = newExpense.vehicleId
                it[driverId] = newExpense.driverId
                it[tripId] = newExpense.tripId
                it[date] = newExpense.date
                it[type] = newExpense.type
                it[description] = newExpense.description
                it[amount] = newExpense.amount.toBigDecimal()
                // 🔹 novos campos de abastecimento
                it[liters] = newExpense.liters?.toBigDecimal()
                it[pricePerLiter] = newExpense.pricePerLiter?.toBigDecimal()
                it[odometer] = newExpense.odometer
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Despesa criada com sucesso")
        )
    }

    suspend fun updateExpense(id: Int, req: String): ServiceResponse<Message> {
        val updatedExpense = validatePartialExpense(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingExpense = DatabaseFactory.dbQuery {
            ExpensesTable
                .selectAll()
                .where { ExpensesTable.id eq id }
                .singleOrNull()
        }

        if (existingExpense == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Despesa não encontrada!")
            )
        }

        DatabaseFactory.dbQuery {
            ExpensesTable.update({ ExpensesTable.id eq id }) {
                updatedExpense.vehicleId?.let { v -> it[vehicleId] = v }
                updatedExpense.driverId?.let { d -> it[driverId] = d }
                updatedExpense.tripId?.let { t -> it[tripId] = t }
                updatedExpense.date?.let { dt -> it[date] = dt }
                updatedExpense.type?.let { tp -> it[type] = tp }
                updatedExpense.description?.let { desc -> it[description] = desc }
                updatedExpense.amount?.let { am -> it[amount] = am.toBigDecimal() }
                // 🔹 novos campos de abastecimento
                updatedExpense.liters?.let { l -> it[liters] = l.toBigDecimal() }
                updatedExpense.pricePerLiter?.let { ppl -> it[pricePerLiter] = ppl.toBigDecimal() }
                updatedExpense.odometer?.let { odo -> it[odometer] = odo }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Despesa atualizada com sucesso")
        )
    }

    suspend fun getAllExpenses(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = ExpensesTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        vehicleIdFilter: Int? = null,
        driverIdFilter: Int? = null,
        tripIdFilter: Int? = null,
        typeFilter: String? = null,
        dateFilter: LocalDate? = null,
        minAmountFilter: Double? = null,
        maxAmountFilter: Double? = null,
        minLitersFilter: Double? = null,
        maxLitersFilter: Double? = null,
        minOdometerFilter: Int? = null,
        maxOdometerFilter: Int? = null
    ): ServiceResponse<PaginatedResponse<Expense>> {
        return DatabaseFactory.dbQuery {
            val query = ExpensesTable
                .selectAll()
                .apply {
                    if (idFilter != null) andWhere { ExpensesTable.id eq idFilter }
                    if (vehicleIdFilter != null) andWhere { ExpensesTable.vehicleId eq vehicleIdFilter }
                    if (driverIdFilter != null) andWhere { ExpensesTable.driverId eq driverIdFilter }
                    if (tripIdFilter != null) andWhere { ExpensesTable.tripId eq tripIdFilter }
                    if (!typeFilter.isNullOrBlank()) andWhere { ExpensesTable.type like "%$typeFilter%" }
                    if (dateFilter != null) andWhere { ExpensesTable.date eq dateFilter }

                    // 🔹 filtros numéricos adicionais
                    if (minAmountFilter != null) andWhere { ExpensesTable.amount greaterEq minAmountFilter.toBigDecimal() }
                    if (maxAmountFilter != null) andWhere { ExpensesTable.amount lessEq maxAmountFilter.toBigDecimal() }
                    if (minLitersFilter != null) andWhere { ExpensesTable.liters greaterEq minLitersFilter.toBigDecimal() }
                    if (maxLitersFilter != null) andWhere { ExpensesTable.liters lessEq maxLitersFilter.toBigDecimal() }
                    if (minOdometerFilter != null) andWhere { ExpensesTable.odometer greaterEq minOdometerFilter }
                    if (maxOdometerFilter != null) andWhere { ExpensesTable.odometer lessEq maxOdometerFilter }
                }

            val total = query.count()

            val results = query
                .orderBy(sortBy to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Expense(
                        id = it[ExpensesTable.id],
                        vehicleId = it[ExpensesTable.vehicleId],
                        driverId = it[ExpensesTable.driverId],
                        tripId = it[ExpensesTable.tripId],
                        date = it[ExpensesTable.date],
                        type = it[ExpensesTable.type],
                        description = it[ExpensesTable.description],
                        amount = it[ExpensesTable.amount].toDouble(),
                        liters = it[ExpensesTable.liters]?.toDouble(),
                        pricePerLiter = it[ExpensesTable.pricePerLiter]?.toDouble(),
                        odometer = it[ExpensesTable.odometer]
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


    suspend fun findExpenseById(id: Int): ServiceResponse<Any> {
        val expense = DatabaseFactory.dbQuery {
            ExpensesTable
                .selectAll()
                .where { ExpensesTable.id eq id }
                .singleOrNull()
                ?.let {
                    Expense(
                        id = it[ExpensesTable.id],
                        vehicleId = it[ExpensesTable.vehicleId],
                        driverId = it[ExpensesTable.driverId],
                        tripId = it[ExpensesTable.tripId],
                        date = it[ExpensesTable.date],
                        type = it[ExpensesTable.type],
                        description = it[ExpensesTable.description],
                        amount = it[ExpensesTable.amount].toDouble(),
                        liters = it[ExpensesTable.liters]?.toDouble(),
                        pricePerLiter = it[ExpensesTable.pricePerLiter]?.toDouble(),
                        odometer = it[ExpensesTable.odometer]
                    )
                }
        }

        return if (expense == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = mapOf("message" to "Despesa não encontrada")
            )
        } else {
            ServiceResponse(
                status = HttpStatusCode.OK,
                data = expense
            )
        }
    }

    suspend fun deleteExpense(id: Int): ServiceResponse<Message> {
        val existingExpense = DatabaseFactory.dbQuery {
            ExpensesTable
                .selectAll()
                .where { ExpensesTable.id eq id }
                .singleOrNull()
        }

        if (existingExpense == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Despesa não encontrada!")
            )
        }

        DatabaseFactory.dbQuery {
            ExpensesTable.deleteWhere { ExpensesTable.id eq id }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Despesa removida com sucesso")
        )
    }
}
