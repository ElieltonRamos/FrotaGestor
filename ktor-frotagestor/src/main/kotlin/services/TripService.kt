package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.DriversTable
import com.frotagestor.database.models.TripsTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateTrip
import com.frotagestor.validations.validatePartialTrip
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class TripService {

    suspend fun createTrip(req: String): ServiceResponse<Message> {
        val newTrip = validateTrip(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        DatabaseFactory.dbQuery {
            TripsTable.insert {
                it[vehicleId] = newTrip.vehicleId
                it[driverId] = newTrip.driverId
                it[startLocation] = newTrip.startLocation
                it[endLocation] = newTrip.endLocation
                it[startTime] = newTrip.startTime
                it[endTime] = newTrip.endTime
                it[distanceKm] = newTrip.distanceKm?.toBigDecimal()
                it[status] = newTrip.status
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Viagem criada com sucesso")
        )
    }

    suspend fun updateTrip(id: Int, req: String): ServiceResponse<Message> {
        val updatedTrip = validatePartialTrip(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingTrip = DatabaseFactory.dbQuery {
            TripsTable
                .selectAll()
                .where { TripsTable.id eq id }
                .singleOrNull()
        }

        if (existingTrip == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Viagem não encontrada!")
            )
        }

        DatabaseFactory.dbQuery {
            TripsTable.update({ TripsTable.id eq id }) {
                updatedTrip.vehicleId?.let { v -> it[vehicleId] = v }
                updatedTrip.driverId?.let { d -> it[driverId] = d }
                updatedTrip.startLocation?.let { s -> it[startLocation] = s }
                updatedTrip.endLocation?.let { e -> it[endLocation] = e }
                updatedTrip.startTime?.let { st -> it[startTime] = st }
                updatedTrip.endTime?.let { et -> it[endTime] = et }
                updatedTrip.distanceKm?.let { km -> it[distanceKm] = km.toBigDecimal() }
                updatedTrip.status?.let { st -> it[status] = st }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Viagem atualizada com sucesso")
        )
    }

    suspend fun getAllTrips(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = TripsTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        vehicleIdFilter: Int? = null,
        driverIdFilter: Int? = null,
        statusFilter: TripStatus? = null,
        startDateFilter: LocalDateTime? = null,
        endDateFilter: LocalDateTime? = null,
        driverNameFilter: String? = null,
        vehiclePlateFilter: String? = null
    ): ServiceResponse<PaginatedResponse<Trip>> {
        return DatabaseFactory.dbQuery {
            val query = TripsTable
                .join(DriversTable, JoinType.INNER, additionalConstraint = { TripsTable.driverId eq DriversTable.id })
                .join(VehiclesTable, JoinType.INNER, additionalConstraint = { TripsTable.vehicleId eq VehiclesTable.id })
                .select(
                    TripsTable.columns + DriversTable.name + VehiclesTable.plate
                )
                .apply {
                    if (idFilter != null) {
                        andWhere { TripsTable.id eq idFilter }
                    }
                    if (vehicleIdFilter != null) {
                        andWhere { TripsTable.vehicleId eq vehicleIdFilter }
                    }
                    if (driverIdFilter != null) {
                        andWhere { TripsTable.driverId eq driverIdFilter }
                    }
                    if (statusFilter != null) {
                        andWhere { TripsTable.status eq statusFilter }
                    }
                    if (startDateFilter != null) {
                        andWhere { TripsTable.startTime greaterEq startDateFilter }
                    }
                    if (endDateFilter != null) {
                        andWhere { TripsTable.endTime lessEq endDateFilter }
                    }
                    if (driverNameFilter != null) {
                        andWhere { DriversTable.name like "%$driverNameFilter%" }
                    }
                    if (vehiclePlateFilter != null) {
                        andWhere { VehiclesTable.plate like "%$vehiclePlateFilter%" }
                    }
                }

            val total = query.count()

            val results = query
                .orderBy(sortBy to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Trip(
                        id = it[TripsTable.id],
                        vehicleId = it[TripsTable.vehicleId],
                        driverId = it[TripsTable.driverId],
                        driverName = it[DriversTable.name],
                        vehiclePlate = it[VehiclesTable.plate],
                        startLocation = it[TripsTable.startLocation],
                        endLocation = it[TripsTable.endLocation],
                        startTime = it[TripsTable.startTime],
                        endTime = it[TripsTable.endTime],
                        distanceKm = it[TripsTable.distanceKm]?.toDouble(),
                        status = it[TripsTable.status]
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


    suspend fun findTripById(id: Int): ServiceResponse<Any> {
        val trip = DatabaseFactory.dbQuery {
            TripsTable
                .join(DriversTable, JoinType.INNER, additionalConstraint = { TripsTable.driverId eq DriversTable.id })
                .join(VehiclesTable, JoinType.INNER, additionalConstraint = { TripsTable.vehicleId eq VehiclesTable.id })
                .select(TripsTable.columns + DriversTable.name + VehiclesTable.plate)
                .where { TripsTable.id eq id }
                .singleOrNull()
                ?.let {
                    Trip(
                        id = it[TripsTable.id],
                        vehicleId = it[TripsTable.vehicleId],
                        driverId = it[TripsTable.driverId],
                        driverName = it[DriversTable.name],
                        vehiclePlate = it[VehiclesTable.plate],
                        startLocation = it[TripsTable.startLocation],
                        endLocation = it[TripsTable.endLocation],
                        startTime = it[TripsTable.startTime],
                        endTime = it[TripsTable.endTime],
                        distanceKm = it[TripsTable.distanceKm]?.toDouble(),
                        status = it[TripsTable.status]
                    )
                }
        }

        return if (trip != null) {
            ServiceResponse(HttpStatusCode.OK, trip)
        } else {
            ServiceResponse(HttpStatusCode.NotFound, mapOf("message" to "Viagem não encontrada"))
        }
    }


    suspend fun deleteTrip(id: Int): ServiceResponse<Message> {
        val existingTrip = DatabaseFactory.dbQuery {
            TripsTable
                .selectAll()
                .where { TripsTable.id eq id }
                .singleOrNull()
        }

        if (existingTrip == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Viagem não encontrada!")
            )
        }

        DatabaseFactory.dbQuery {
            TripsTable.deleteWhere { TripsTable.id eq id }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Viagem removida com sucesso")
        )
    }
}
