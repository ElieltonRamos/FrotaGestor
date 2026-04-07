package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.*
import com.frotagestor.protocols_devices_gps.gt06.GT06ConnectionManager
import com.frotagestor.protocols_devices_gps.gt06.buildGT06CommandText
import com.frotagestor.protocols_devices_gps.gt06.BuildCommandResult as GT06CommandResult
import com.frotagestor.protocols_devices_gps.suntech.BuildCommandResult as SuntechCommandResult
import com.frotagestor.protocols_devices_gps.suntech.DeviceConnectionManager
import com.frotagestor.protocols_devices_gps.suntech.buildSuntechCommand
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateCommandRequest
import com.frotagestor.validations.validateGpsDevice
import com.frotagestor.validations.validatePartialGpsDevice
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.time.Duration.Companion.hours

class GpsDeviceService {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun rowToGpsDevice(row: ResultRow) = GpsDevice(
        id                = row[GpsDevicesTable.id],
        vehicleId         = row[GpsDevicesTable.vehicleId],
        imei              = row[GpsDevicesTable.imei],
        latitude          = row[GpsDevicesTable.latitude].toDouble(),
        longitude         = row[GpsDevicesTable.longitude].toDouble(),
        dateTime          = row[GpsDevicesTable.dateTime],
        speed             = row[GpsDevicesTable.speed].toDouble(),
        heading           = row[GpsDevicesTable.heading].toDouble(),
        iconMapUrl        = row[GpsDevicesTable.iconMapUrl],
        title             = row[GpsDevicesTable.title],
        ignition          = row[GpsDevicesTable.ignition],
        lastCommunication = row[GpsDevicesTable.lastCommunication],
        batteryVoltage    = row[GpsDevicesTable.batteryVoltage]?.toDouble()
    )

    private fun rowToGpsHistory(row: ResultRow) = GpsHistory(
        id            = row[GpsHistoryTable.id],
        gpsDeviceId   = row[GpsHistoryTable.gpsDeviceId],
        vehicleId     = row[GpsHistoryTable.vehicleId],
        dateTime      = row[GpsHistoryTable.dateTime],
        latitude      = row[GpsHistoryTable.latitude].toDouble(),
        longitude     = row[GpsHistoryTable.longitude].toDouble(),
        speed         = row[GpsHistoryTable.speed].toDouble(),
        heading       = row[GpsHistoryTable.heading].toDouble(),
        ignition      = row[GpsHistoryTable.ignition],
        satellites    = row[GpsHistoryTable.satellites],
        gpsFixed      = row[GpsHistoryTable.gpsFixed],
        gpsQuality    = row[GpsHistoryTable.gpsQuality],
        odometer      = row[GpsHistoryTable.odometer],
        batteryVoltage = row[GpsHistoryTable.batteryVoltage]?.toDouble(),
        messageType   = row[GpsHistoryTable.messageType],
        eventCode     = row[GpsHistoryTable.eventCode],
        rawLog        = row[GpsHistoryTable.rawLog]
    )

    private fun safePagination(page: Int, limit: Int, maxLimit: Int = 100): Triple<Int, Int, Long> {
        val safePage  = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, maxLimit)
        val offset    = ((safePage - 1) * safeLimit).toLong()
        return Triple(safePage, safeLimit, offset)
    }

    private fun defaultDateRange(tz: TimeZone): Pair<LocalDateTime, LocalDateTime> {
        val today = Clock.System.todayIn(tz)
        return today.atStartOfDayIn(tz).toLocalDateTime(tz) to today.atTime(23, 59, 59, 999_999_999)
    }

    // ─── Devices ──────────────────────────────────────────────────────────────

    suspend fun getDevicesWithoutPower(): ServiceResponse<List<GpsDevice>> {
        return DatabaseFactory.dbQuery {
            val twoHoursAgo = Clock.System.now()
                .minus(2.hours)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val results = GpsDevicesTable
                .selectAll()
                .where { GpsDevicesTable.lastCommunication less twoHoursAgo }
                .orderBy(GpsDevicesTable.lastCommunication to SortOrder.DESC)
                .map { rowToGpsDevice(it) }

            ServiceResponse(HttpStatusCode.OK, results)
        }
    }

    suspend fun createGpsDevice(req: String): ServiceResponse<Message> {
        val newDevice = validateGpsDevice(req).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, Message(msg))
        }

        if (newDevice.imei.isNullOrBlank())
            return ServiceResponse(HttpStatusCode.BadRequest, Message("O campo IMEI é obrigatório"))

        DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.imei eq newDevice.imei!! }.singleOrNull()
        }?.let { return ServiceResponse(HttpStatusCode.Conflict, Message("Dispositivo já cadastrado!")) }

        if (newDevice.vehicleId != null) {
            DatabaseFactory.dbQuery {
                GpsDevicesTable.selectAll().where { GpsDevicesTable.vehicleId eq newDevice.vehicleId }.singleOrNull()
            }?.let { return ServiceResponse(HttpStatusCode.Conflict, Message("Já existe um dispositivo vinculado a este veículo!")) }
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.insert { row ->
                row[imei]              = newDevice.imei!!
                row[vehicleId]         = newDevice.vehicleId
                row[latitude]          = (newDevice.latitude ?: 0.0).toBigDecimal()
                row[longitude]         = (newDevice.longitude ?: 0.0).toBigDecimal()
                row[speed]             = (newDevice.speed ?: 0.0).toBigDecimal()
                row[heading]           = (newDevice.heading ?: 0.0).toBigDecimal()
                row[dateTime]          = newDevice.dateTime
                row[iconMapUrl]        = newDevice.iconMapUrl
                row[title]             = newDevice.title
                row[ignition]          = newDevice.ignition ?: false
                row[lastCommunication] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                row[batteryVoltage]    = 0.toBigDecimal()
            }
        }

        return ServiceResponse(HttpStatusCode.Created, Message("Dispositivo GPS criado com sucesso"))
    }

    suspend fun updateGpsDevice(id: Int, req: String): ServiceResponse<Message> {
        val updatedDevice = validatePartialGpsDevice(req).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, Message(msg))
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()
        } ?: return ServiceResponse(HttpStatusCode.NotFound, Message("Dispositivo GPS não encontrado!"))

        if (updatedDevice.vehicleId != null) {
            DatabaseFactory.dbQuery {
                GpsDevicesTable.selectAll()
                    .where {
                        (GpsDevicesTable.vehicleId eq updatedDevice.vehicleId) and
                                (GpsDevicesTable.id neq id)
                    }
                    .singleOrNull()
            }?.let { return ServiceResponse(HttpStatusCode.Conflict, Message("Já existe um dispositivo vinculado a este veículo!")) }
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.update({ GpsDevicesTable.id eq id }) { row ->
                row[vehicleId] = updatedDevice.vehicleId  // null = desvincula
                updatedDevice.imei?.let       { row[imei]      = it }
                updatedDevice.latitude?.let   { row[latitude]  = it.toBigDecimal() }
                updatedDevice.longitude?.let  { row[longitude] = it.toBigDecimal() }
                updatedDevice.speed?.let      { row[speed]     = it.toBigDecimal() }
                updatedDevice.heading?.let    { row[heading]   = it.toBigDecimal() }
                updatedDevice.dateTime?.let   { row[dateTime]  = it }
                updatedDevice.iconMapUrl?.let { row[iconMapUrl] = it }
                updatedDevice.title?.let      { row[title]     = it }
                updatedDevice.ignition?.let   { row[ignition]  = it }
            }
        }

        return ServiceResponse(HttpStatusCode.OK, Message("Dispositivo GPS atualizado com sucesso"))
    }

    suspend fun deleteGpsDevice(id: Int): ServiceResponse<Message> {
        val existing = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()
        } ?: return ServiceResponse(HttpStatusCode.NotFound, Message("Dispositivo GPS não encontrado!"))

        if (existing[GpsDevicesTable.vehicleId] != null)
            return ServiceResponse(
                HttpStatusCode.BadRequest,
                Message("Não é possível deletar um dispositivo vinculado a um veículo. Desvincule-o primeiro.")
            )

        DatabaseFactory.dbQuery { GpsDevicesTable.deleteWhere { GpsDevicesTable.id eq id } }

        return ServiceResponse(HttpStatusCode.OK, Message("Dispositivo GPS deletado com sucesso"))
    }

    suspend fun getAllGpsDevices(
        page: Int = 1,
        limit: Int = 10,
        vehicleIdFilter: Int? = null,
        imeiFilter: String? = null
    ): ServiceResponse<PaginatedResponse<GpsDevice>> {
        return DatabaseFactory.dbQuery {
            val (safePage, safeLimit, offset) = safePagination(page, limit)

            val query = GpsDevicesTable.selectAll().apply {
                vehicleIdFilter?.let { andWhere { GpsDevicesTable.vehicleId eq it } }
                imeiFilter?.let      { andWhere { GpsDevicesTable.imei eq it } }
            }

            val total   = query.count()
            val results = query
                .orderBy(GpsDevicesTable.id to SortOrder.DESC)
                .limit(safeLimit)
                .offset(offset)
                .map { rowToGpsDevice(it) }

            ServiceResponse(
                HttpStatusCode.OK,
                PaginatedResponse(
                    data       = results,
                    total      = total.toInt(),
                    page       = safePage,
                    limit      = safeLimit,
                    totalPages = if (total == 0L) 0 else ((total + safeLimit - 1) / safeLimit).toInt()
                )
            )
        }
    }

    suspend fun findGpsDeviceById(id: Int): ServiceResponse<Any> {
        val device = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()?.let { rowToGpsDevice(it) }
        }
        return if (device == null)
            ServiceResponse(HttpStatusCode.NotFound, mapOf("message" to "Dispositivo GPS não encontrado"))
        else
            ServiceResponse(HttpStatusCode.OK, device)
    }

    suspend fun findGpsDeviceByVehicleId(vehicleId: Int): ServiceResponse<Any> {
        val device = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.vehicleId eq vehicleId }.singleOrNull()?.let { rowToGpsDevice(it) }
        }
        return if (device == null)
            ServiceResponse(HttpStatusCode.NotFound, mapOf("message" to "Dispositivo GPS não encontrado para o veículo informado"))
        else
            ServiceResponse(HttpStatusCode.OK, device)
    }

    // ─── History ──────────────────────────────────────────────────────────────

    suspend fun getHistoryByVehicle(
        vehicleId: Int,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        page: Int = 1,
        limit: Int = 20
    ): ServiceResponse<PaginatedResponse<GpsHistory>> {
        return DatabaseFactory.dbQuery {
            val tz = TimeZone.currentSystemDefault()
            val (defaultStart, defaultEnd) = defaultDateRange(tz)
            val from = startDate ?: defaultStart
            val to   = endDate   ?: defaultEnd
            val (safePage, safeLimit, offset) = safePagination(page, limit)

            val baseQuery = GpsHistoryTable
                .selectAll()
                .where {
                    (GpsHistoryTable.vehicleId eq vehicleId) and
                            (GpsHistoryTable.dateTime greaterEq from) and
                            (GpsHistoryTable.dateTime lessEq to)
                }

            val total   = baseQuery.count()
            val results = baseQuery
                .orderBy(GpsHistoryTable.dateTime to SortOrder.ASC)
                .limit(safeLimit)
                .offset(offset)
                .map { rowToGpsHistory(it) }

            ServiceResponse(
                HttpStatusCode.OK,
                PaginatedResponse(
                    data       = results,
                    total      = total.toInt(),
                    page       = safePage,
                    limit      = safeLimit,
                    totalPages = if (total == 0L) 0 else ((total + safeLimit - 1) / safeLimit).toInt()
                )
            )
        }
    }

    // ─── Subfleet ─────────────────────────────────────────────────────────────

    suspend fun getGpsDevicesBySubfleet(subfleetId: Int): ServiceResponse<List<GpsDevice>> {
        return DatabaseFactory.dbQuery {
            val results = GpsDevicesTable
                .join(VehiclesTable, JoinType.INNER, additionalConstraint = { GpsDevicesTable.vehicleId eq VehiclesTable.id })
                .selectAll()
                .where { VehiclesTable.subfleetId eq subfleetId }
                .map { rowToGpsDevice(it) }

            ServiceResponse(HttpStatusCode.OK, results)
        }
    }

    suspend fun getGpsHistoryBySubfleet(
        subfleetId: Int,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        page: Int = 1,
        limit: Int = 20
    ): ServiceResponse<PaginatedResponse<GpsHistory>> {
        return DatabaseFactory.dbQuery {
            val tz = TimeZone.currentSystemDefault()
            val (defaultStart, defaultEnd) = defaultDateRange(tz)
            val from = startDate ?: defaultStart
            val to   = endDate   ?: defaultEnd
            val (safePage, safeLimit, offset) = safePagination(page, limit)

            val baseQuery = GpsHistoryTable
                .join(VehiclesTable, JoinType.INNER, additionalConstraint = { GpsHistoryTable.vehicleId eq VehiclesTable.id })
                .selectAll()
                .where {
                    (VehiclesTable.subfleetId eq subfleetId) and
                            (GpsHistoryTable.dateTime greaterEq from) and
                            (GpsHistoryTable.dateTime lessEq to)
                }

            val total   = baseQuery.count()
            val results = baseQuery
                .orderBy(GpsHistoryTable.dateTime to SortOrder.DESC)
                .limit(safeLimit)
                .offset(offset)
                .map { rowToGpsHistory(it) }

            ServiceResponse(
                HttpStatusCode.OK,
                PaginatedResponse(
                    data       = results,
                    total      = total.toInt(),
                    page       = safePage,
                    limit      = safeLimit,
                    totalPages = if (total == 0L) 0 else ((total + safeLimit - 1) / safeLimit).toInt()
                )
            )
        }
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    suspend fun sendCommandDevice(rawBody: String): ServiceResponse<CommandResponse> {
        val request = validateCommandRequest(rawBody).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, CommandResponse(false, msg))
        }
        val deviceId = request.deviceId

        DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.imei eq deviceId }.singleOrNull()
        } ?: return ServiceResponse(HttpStatusCode.NotFound, CommandResponse(false, "Dispositivo não encontrado: $deviceId"))

        return if (deviceId.length == 15) sendGT06Command(deviceId, request)
        else sendSuntechCommand(deviceId, request)
    }

    private suspend fun sendGT06Command(deviceId: String, request: CommandRequest): ServiceResponse<CommandResponse> {
        if (!GT06ConnectionManager.isDeviceConnected(deviceId))
            return ServiceResponse(HttpStatusCode.ServiceUnavailable, CommandResponse(false, "Dispositivo GT06 não está conectado ao servidor TCP"))

        return when (val result = buildGT06CommandText(request)) {
            is GT06CommandResult.Success -> {
                val sent = GT06ConnectionManager.sendCommand(deviceId, result.command)
                if (sent)
                    ServiceResponse(HttpStatusCode.OK, CommandResponse(true, "Comando enviado", result.command))
                else
                    ServiceResponse(HttpStatusCode.ServiceUnavailable, CommandResponse(false, "Falha ao enviar comando GT06"))
            }
            is GT06CommandResult.Error ->
                ServiceResponse(HttpStatusCode.BadRequest, CommandResponse(false, result.message))
        }
    }

    private suspend fun sendSuntechCommand(deviceId: String, request: CommandRequest): ServiceResponse<CommandResponse> {
        if (!DeviceConnectionManager.isDeviceConnected(deviceId))
            return ServiceResponse(HttpStatusCode.ServiceUnavailable, CommandResponse(false, "Dispositivo Suntech não está conectado ao servidor TCP"))

        return when (val result = buildSuntechCommand(deviceId, request)) {
            is SuntechCommandResult.Success -> {
                val sent = DeviceConnectionManager.sendCommand(deviceId, result.command)
                if (sent)
                    ServiceResponse(HttpStatusCode.OK, CommandResponse(true, "Comando enviado", result.command))
                else
                    ServiceResponse(HttpStatusCode.ServiceUnavailable, CommandResponse(false, "Falha ao enviar comando Suntech"))
            }
            is SuntechCommandResult.Error ->
                ServiceResponse(HttpStatusCode.BadRequest, CommandResponse(false, result.message))
        }
    }
}