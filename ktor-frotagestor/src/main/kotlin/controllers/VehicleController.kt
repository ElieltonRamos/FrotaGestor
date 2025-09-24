package com.frotagestor.controllers

import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.VehicleStatus
import com.frotagestor.services.VehicleService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.SortOrder

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

            // 📌 Chama o service
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
}
