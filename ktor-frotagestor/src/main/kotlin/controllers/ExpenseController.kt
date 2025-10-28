package com.frotagestor.controllers

import com.frotagestor.database.models.DriversTable
import com.frotagestor.database.models.ExpensesTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.ExpenseIndicators
import com.frotagestor.interfaces.MaintenanceIndicators
import com.frotagestor.interfaces.RefuelingIndicators
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.services.ExpenseService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.SortOrder
import kotlinx.datetime.LocalDate
import kotlin.system.measureTimeMillis

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

            val dateStartFilter = call.request.queryParameters["dateStart"]?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
            val dateEndFilter = call.request.queryParameters["dateEnd"]?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            val typeFilter = call.request.queryParameters["type"]
            val descriptionFilter = call.request.queryParameters["description"]

            val minAmountFilter = call.request.queryParameters["minAmount"]?.toDoubleOrNull()
            val maxAmountFilter = call.request.queryParameters["maxAmount"]?.toDoubleOrNull()
            val minLitersFilter = call.request.queryParameters["minLiters"]?.toDoubleOrNull()
            val maxLitersFilter = call.request.queryParameters["maxLiters"]?.toDoubleOrNull()
            val minOdometerFilter = call.request.queryParameters["minOdometer"]?.toIntOrNull()
            val maxOdometerFilter = call.request.queryParameters["maxOdometer"]?.toIntOrNull()
            val minPricePerLiterFilter = call.request.queryParameters["minPricePerLiter"]?.toDoubleOrNull()
            val maxPricePerLiterFilter = call.request.queryParameters["maxPricePerLiter"]?.toDoubleOrNull()

            val driverNameFilter = call.request.queryParameters["driverName"]
            val vehiclePlateFilter = call.request.queryParameters["vehiclePlate"]

            val sortByColumn = when (sortByParam.lowercase()) {
                "vehicleid" -> ExpensesTable.vehicleId
                "driverid" -> ExpensesTable.driverId
                "tripid" -> ExpensesTable.tripId
                "date" -> ExpensesTable.date
                "type" -> ExpensesTable.type
                "description" -> ExpensesTable.description
                "amount" -> ExpensesTable.amount
                "liters" -> ExpensesTable.liters
                "priceperliter" -> ExpensesTable.pricePerLiter
                "odometer" -> ExpensesTable.odometer
                "drivername" -> DriversTable.name
                "vehicleplate" -> VehiclesTable.plate
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
                dateStartFilter = dateStartFilter,
                dateEndFilter = dateEndFilter,
                typeFilter = typeFilter,
                descriptionFilter = descriptionFilter,
                minAmountFilter = minAmountFilter,
                maxAmountFilter = maxAmountFilter,
                minLitersFilter = minLitersFilter,
                maxLitersFilter = maxLitersFilter,
                minOdometerFilter = minOdometerFilter,
                maxOdometerFilter = maxOdometerFilter,
                minPricePerLiterFilter = minPricePerLiterFilter,
                maxPricePerLiterFilter = maxPricePerLiterFilter,
                driverNameFilter = driverNameFilter,
                vehiclePlateFilter = vehiclePlateFilter
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

    suspend fun getRefuelingIndicators(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]

            val startDate = startDateParam?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            val endDate = endDateParam?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            var serviceResult: ServiceResponse<RefuelingIndicators>
            val timeMillis = measureTimeMillis {
                serviceResult = expenseService.getRefuelingIndicators(startDate, endDate)
            }
            println("⏱ Refueling Indicators service execution time: ${timeMillis}ms")

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("❌ Error in getRefuelingIndicators route: ${e.stackTraceToString()}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno ao gerar indicadores de abastecimento.")
            )
        }
    }

    suspend fun getMaintenanceIndicators(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]

            val startDate = startDateParam?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            val endDate = endDateParam?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            var serviceResult: ServiceResponse<MaintenanceIndicators>
            val timeMillis = measureTimeMillis {
                serviceResult = expenseService.getMaintenanceIndicators(startDate, endDate)
            }
            println("⏱ Maintenance Indicators service execution time: ${timeMillis}ms")

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("❌ Error in getMaintenanceIndicators route: ${e.stackTraceToString()}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno ao gerar indicadores de manutenção.")
            )
        }
    }

    suspend fun getExpenseIndicators(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]

            val startDate = startDateParam?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            val endDate = endDateParam?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            var serviceResult: ServiceResponse<ExpenseIndicators>
            val timeMillis = measureTimeMillis {
                serviceResult = expenseService.getExpenseIndicators(startDate, endDate)
            }
            println("⏱ Expense Indicators service execution time: ${timeMillis}ms")

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("❌ Error in getExpenseIndicators route: ${e.stackTraceToString()}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to "Erro interno ao gerar indicadores de despesas.")
            )
        }
    }
}