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
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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

    suspend fun getTripIndicators(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<TripIndicators> = DatabaseFactory.dbQuery {

        // Define intervalo de datas (mês atual, se não for informado)
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val timeZone = TimeZone.currentSystemDefault()

        val startDateTime = start.atStartOfDayIn(timeZone).toLocalDateTime(timeZone)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone).toLocalDateTime(timeZone)

        // Query SQL única com agregações e última viagem
        val sql = """
        SELECT
            COUNT(t.id) AS total_trips,
            SUM(CASE WHEN t.status = 'PLANEJADA' THEN 1 ELSE 0 END) AS planned,
            SUM(CASE WHEN t.status = 'EM_ANDAMENTO' THEN 1 ELSE 0 END) AS in_progress,
            SUM(CASE WHEN t.status = 'CONCLUIDA' THEN 1 ELSE 0 END) AS completed,
            SUM(CASE WHEN t.status = 'CANCELADA' THEN 1 ELSE 0 END) AS canceled,
            COALESCE(SUM(t.distance_km), 0) AS total_distance,
            COALESCE(AVG(t.distance_km), 0) AS avg_distance,
            MAX(t.start_time) AS last_trip_date,
            (
                SELECT d.name
                FROM trips t2
                JOIN drivers d ON t2.driver_id = d.id
                WHERE t2.start_time = MAX(t.start_time)
                LIMIT 1
            ) AS last_driver_name,
            (
                SELECT v.plate
                FROM trips t3
                JOIN vehicles v ON t3.vehicle_id = v.id
                WHERE t3.start_time = MAX(t.start_time)
                LIMIT 1
            ) AS last_vehicle_plate
        FROM trips t
        WHERE t.start_time BETWEEN '$startDateTime' AND '$endDateTime';
    """.trimIndent()

        var totalTrips = 0
        var planned = 0
        var inProgress = 0
        var completed = 0
        var canceled = 0
        var totalDistance = 0.0
        var avgDistance = 0.0
        var lastTrip: LastTrip? = null

        transaction {
            exec(sql) { rs ->
                if (rs.next()) {
                    totalTrips = rs.getInt("total_trips")
                    planned = rs.getInt("planned")
                    inProgress = rs.getInt("in_progress")
                    completed = rs.getInt("completed")
                    canceled = rs.getInt("canceled")
                    totalDistance = rs.getDouble("total_distance")
                    avgDistance = rs.getDouble("avg_distance")

                    val date = rs.getString("last_trip_date")
                    val driver = rs.getString("last_driver_name")
                    val plate = rs.getString("last_vehicle_plate")

                    if (date != null && driver != null && plate != null) {
                        lastTrip = LastTrip(
                            date = date,
                            driverName = driver,
                            vehiclePlate = plate
                        )
                    }
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = TripIndicators(
                totalTrips = totalTrips,
                planned = planned,
                inProgress = inProgress,
                completed = completed,
                canceled = canceled,
                totalDistance = totalDistance,
                avgDistance = avgDistance,
                lastTrip = lastTrip
            )
        )
    }
}
