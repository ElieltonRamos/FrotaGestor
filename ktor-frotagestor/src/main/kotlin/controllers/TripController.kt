package com.frotagestor.controllers

import com.frotagestor.database.models.DriversTable
import com.frotagestor.database.models.TripsTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.TripIndicators
import com.frotagestor.interfaces.TripStatus
import com.frotagestor.services.TripService
import com.frotagestor.plugins.RawBodyKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.SortOrder
import kotlinx.datetime.LocalDateTime
import kotlin.system.measureTimeMillis

class TripController(private val tripService: TripService) {
    private val internalMsgError = "Internal server error"

    suspend fun create(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val serviceResult = tripService.createTrip(rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in create trip route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun update(call: ApplicationCall) {
        try {
            val rawBody = call.attributes[RawBodyKey]
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = tripService.updateTrip(id, rawBody)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in update trip route: ${e.message}")
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
            val driverNameFilter = call.request.queryParameters["driverName"]
            val vehiclePlateFilter = call.request.queryParameters["vehiclePlate"]

            val statusFilter = call.request.queryParameters["status"]?.let {
                runCatching { TripStatus.valueOf(it.uppercase()) }.getOrNull()
            }

            val startDateFilter = call.request.queryParameters["startDate"]?.let {
                runCatching { LocalDateTime.parse(it) }.getOrNull()
            }
            val endDateFilter = call.request.queryParameters["endDate"]?.let {
                runCatching { LocalDateTime.parse(it) }.getOrNull()
            }

            val sortByColumn = when (sortByParam.lowercase()) {
                "vehicleid" -> TripsTable.vehicleId
                "driverid" -> TripsTable.driverId
                "startlocation" -> TripsTable.startLocation
                "endlocation" -> TripsTable.endLocation
                "starttime" -> TripsTable.startTime
                "endtime" -> TripsTable.endTime
                "distancekm" -> TripsTable.distanceKm
                "status" -> TripsTable.status
                "drivername" -> DriversTable.name
                "vehicleplate" -> VehiclesTable.plate
                else -> TripsTable.id
            }


            val sortOrder = if (orderParam.equals("desc", ignoreCase = true)) {
                SortOrder.DESC
            } else {
                SortOrder.ASC
            }

            val serviceResult = tripService.getAllTrips(
                page = page,
                limit = limit,
                sortBy = sortByColumn,
                sortOrder = sortOrder,
                idFilter = idFilter,
                vehicleIdFilter = vehicleIdFilter,
                driverIdFilter = driverIdFilter,
                statusFilter = statusFilter,
                startDateFilter = startDateFilter,
                endDateFilter = endDateFilter,
                driverNameFilter = driverNameFilter,
                vehiclePlateFilter = vehiclePlateFilter
            )

            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getAll trips route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }


    suspend fun getById(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = tripService.findTripById(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getById trip route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun delete(call: ApplicationCall) {
        try {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Parâmetro 'id' inválido ou ausente"))

            val serviceResult = tripService.deleteTrip(id)
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in delete trip route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }

    suspend fun getTripIndicators(call: ApplicationCall) {
        try {
            val startDateParam = call.request.queryParameters["startDate"]
            val endDateParam = call.request.queryParameters["endDate"]
            val startDate = startDateParam?.let {
                runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull()
            }
            val endDate = endDateParam?.let {
                runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull()
            }
            var serviceResult: ServiceResponse<TripIndicators>
            val timeMillis = measureTimeMillis {
                serviceResult = tripService.getTripIndicators(startDate = startDate, endDate = endDate)
            }
            println("⏱ Trip Indicators service execution time: ${timeMillis}ms")
            call.respond(serviceResult.status, serviceResult.data)
        } catch (e: Exception) {
            println("Error in getTripIndicators route: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to internalMsgError))
        }
    }
}
