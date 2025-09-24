package com.frotagestor.controllers

import com.frotagestor.database.models.ExpensesTable
import com.frotagestor.services.ExpenseService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.SortOrder
import kotlinx.datetime.LocalDate

class ExpenseController(private val expenseService: ExpenseService) {
    private val internalMsgError = "Internal server error"

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = expenseService.createExpense(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create expense route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = expenseService.updateExpense(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update expense route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getAll(call: ApplicationCall) {
        try {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val sortByParam = call.request.queryParameters["sortBy"] ?: "id"
            val orderParam = call.request.queryParameters["order"] ?: "asc"

            val idFilter = call.request.queryParameters["id"]?.toIntOrNull()
            val vehicleIdFilter = call.request.queryParameters["vehicleId"]?.toIntOrNull()
            val driverIdFilter = call.request.queryParameters["driverId"]?.toIntOrNull()
            val tripIdFilter = call.request.queryParameters["tripId"]?.toIntOrNull()
            val dateFilter = call.request.queryParameters["date"]?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
            val typeFilter = call.request.queryParameters["type"]

            val sortByColumn = when (sortByParam.lowercase()) {
                "vehicleid" -> ExpensesTable.vehicleId
                "driverid" -> ExpensesTable.driverId
                "tripid" -> ExpensesTable.tripId
                "date" -> ExpensesTable.date
                "type" -> ExpensesTable.type
                "amount" -> ExpensesTable.amount
                else -> ExpensesTable.id
            }

            val sortOrder = if (orderParam.equals("desc", ignoreCase = true)) {
                SortOrder.DESC
            } else {
                SortOrder.ASC
            }

            val serviceResult = expenseService.getAllExpenses(
                page = page,
                limit = limit,
                sortBy = sortByColumn,
                sortOrder = sortOrder,
                idFilter = idFilter,
                vehicleIdFilter = vehicleIdFilter,
                driverIdFilter = driverIdFilter,
                tripIdFilter = tripIdFilter,
                dateFilter = dateFilter,
                typeFilter = typeFilter
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll expense route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = expenseService.findExpenseById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById expense route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun delete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val serviceResult = expenseService.deleteExpense(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in delete expense route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}
