package com.frotagestor.controllers

import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.VehicleReport
import com.frotagestor.interfaces.VehicleStatus
import com.frotagestor.services.VehicleService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.SortOrder
import kotlin.system.measureTimeMillis

class VehicleController(private val vehicleService: VehicleService) {
    private val internalMsgError = "Internal server error"

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = vehicleService.createVehicle(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create vehicle route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = vehicleService.updateVehicle(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update vehicle route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getAll(call: ApplicationCall) {
        try {
            // 📌 Query params básicos
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val sortByParam = call.request.queryParameters["sortBy"] ?: "id"
            val orderParam = call.request.queryParameters["order"] ?: "asc"

            // 📌 Filtros
            val idFilter = call.request.queryParameters["id"]?.toIntOrNull()
            val plateFilter = call.request.queryParameters["plate"]
            val modelFilter = call.request.queryParameters["model"]
            val brandFilter = call.request.queryParameters["brand"]
            val yearFilter = call.request.queryParameters["year"]?.toIntOrNull()
            val statusFilter = call.request.queryParameters["status"]?.let {
                runCatching { VehicleStatus.valueOf(it.uppercase()) }.getOrNull()
            }

            // 📌 Mapeia string -> coluna
            val sortByColumn = when (sortByParam.lowercase()) {
                "plate" -> VehiclesTable.plate
                "model" -> VehiclesTable.model
                "brand" -> VehiclesTable.brand
                "year" -> VehiclesTable.year
                "status" -> VehiclesTable.status
                else -> VehiclesTable.id
            }

            val sortOrder = if (orderParam.equals("desc", ignoreCase = true)) {
                SortOrder.DESC
            } else {
                SortOrder.ASC
            }

            val serviceResult = vehicleService.getAllVehicles(
                page = page,
                limit = limit,
                sortBy = sortByColumn,
                sortOrder = sortOrder,
                idFilter = idFilter,
                plateFilter = plateFilter,
                modelFilter = modelFilter,
                brandFilter = brandFilter,
                yearFilter = yearFilter,
                statusFilter = statusFilter
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll vehicle route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = vehicleService.findVehicleById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById vehicle route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun softDelete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = vehicleService.softDeleteVehicle(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in softDelete vehicle route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getIndicators(call: ApplicationCall) {
        try {
            val serviceResult = vehicleService.getIndicators()
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getIndicators vehicle route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getTripsByVehicle(call: ApplicationCall) {
        try {
            val vehicleId = call.parameters["id"]?.toIntOrNull()
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            if (vehicleId == null || startDateParam == null || endDateParam == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetros obrigatórios: id, startDate, endDate")
                )
                return
            }

            val startDate = try {
                LocalDate.parse(startDateParam)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Formato inválido de startDate. Use YYYY-MM-DD.")
                )
                return
            }

            val endDate = try {
                LocalDate.parse(endDateParam)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Formato inválido de endDate. Use YYYY-MM-DD.")
                )
                return
            }

            val serviceResult = vehicleService.getTripsByVehicle(
                vehicleId = vehicleId,
                startDate = startDate,
                endDate = endDate,
                page = page,
                limit = limit
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getTripsByVehicle route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }

    suspend fun getExpensesByVehicle(call: ApplicationCall) {
        try {
            val vehicleId = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            if (startDateParam == null || endDateParam == null) {
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetros obrigatórios: startDate e endDate")
                )
            }

            val startDate = try { LocalDate.parse(startDateParam) } catch (_: Exception) {
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Formato inválido de startDate. Use YYYY-MM-DD")
                )
            }

            val endDate = try { LocalDate.parse(endDateParam) } catch (_: Exception) {
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Formato inválido de endDate. Use YYYY-MM-DD")
                )
            }

            val serviceResult = vehicleService.getExpensesByVehicle(
                vehicleId = vehicleId,
                startDate = startDate,
                endDate = endDate,
                page = page,
                limit = limit
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getExpensesByVehicle route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }

    suspend fun getTopDriver(call: ApplicationCall) {
        try {
            val vehicleId = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetro 'id' inválido ou ausente")
                )

            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]

            if (startDateParam == null || endDateParam == null) {
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Parâmetros obrigatórios: startDate e endDate")
                )
            }

            val startDate = try { LocalDate.parse(startDateParam) } catch (_: Exception) {
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Formato inválido de startDate. Use YYYY-MM-DD")
                )
            }

            val endDate = try { LocalDate.parse(endDateParam) } catch (_: Exception) {
                return call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Formato inválido de endDate. Use YYYY-MM-DD")
                )
            }

            val serviceResult = vehicleService.getTopDriverByVehicle(vehicleId, startDate, endDate)
            call.respond(serviceResult.status, serviceResult.data)

        } catch (e: Exception) {
            println("Error in getTopDriver route: ${e.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("message" to internalMsgError)
            )
        }
    }
}
