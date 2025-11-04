package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateGpsDevice
import com.frotagestor.validations.validatePartialGpsDevice
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class GpsDeviceService {

    suspend fun createGpsDevice(req: String): ServiceResponse<Message> {
        val newDevice = validateGpsDevice(req).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, Message(msg))
        }

        if (newDevice.imei.isNullOrBlank()) {
            return ServiceResponse(HttpStatusCode.BadRequest, Message("O campo IMEI é obrigatório"))
        }

        // Verifica se já existe dispositivo com este IMEI
        val existingDevice = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.imei eq newDevice.imei!! }.singleOrNull()
        }
        if (existingDevice != null) {
            return ServiceResponse(HttpStatusCode.Conflict, Message("Dispositivo já cadastrado!"))
        }

        // Verifica se o veículo já possui um dispositivo (apenas se vehicleId for fornecido)
        if (newDevice.vehicleId != null) {
            val existingDeviceForVehicle = DatabaseFactory.dbQuery {
                GpsDevicesTable.selectAll()
                    .where { GpsDevicesTable.vehicleId eq newDevice.vehicleId }
                    .singleOrNull()
            }
            if (existingDeviceForVehicle != null) {
                return ServiceResponse(HttpStatusCode.Conflict, Message("Já existe um dispositivo vinculado a este veículo!"))
            }
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.insert { row ->
                row[imei] = newDevice.imei!!
                row[vehicleId] = newDevice.vehicleId // CORRIGIDO: aceita null
                row[latitude] = (newDevice.latitude ?: 0.0).toBigDecimal()
                row[longitude] = (newDevice.longitude ?: 0.0).toBigDecimal()
                row[speed] = (newDevice.speed ?: 0.0).toBigDecimal()
                row[heading] = (newDevice.heading ?: 0.0).toBigDecimal()
                row[dateTime] = newDevice.dateTime
                row[iconMapUrl] = newDevice.iconMapUrl
                row[title] = newDevice.title
                row[ignition] = newDevice.ignition ?: false
            }
        }

        return ServiceResponse(HttpStatusCode.Created, Message("Dispositivo GPS criado com sucesso"))
    }

    suspend fun updateGpsDevice(id: Int, req: String): ServiceResponse<Message> {
        val updatedDevice = validatePartialGpsDevice(req).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, Message(msg))
        }

        val existingDevice = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()
        }

        if (existingDevice == null) {
            return ServiceResponse(HttpStatusCode.NotFound, Message("Dispositivo GPS não encontrado!"))
        }

        // Se está tentando vincular a um veículo, verifica se já existe outro dispositivo nesse veículo
        if (updatedDevice.vehicleId != null) {
            val existingDeviceForVehicle = DatabaseFactory.dbQuery {
                GpsDevicesTable.selectAll()
                    .where {
                        (GpsDevicesTable.vehicleId eq updatedDevice.vehicleId) and
                                (GpsDevicesTable.id neq id)
                    }
                    .singleOrNull()
            }
            if (existingDeviceForVehicle != null) {
                return ServiceResponse(HttpStatusCode.Conflict, Message("Já existe um dispositivo vinculado a este veículo!"))
            }
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.update({ GpsDevicesTable.id eq id }) { row ->
                // Permite atualizar para null (desvincular)
                if (updatedDevice.vehicleId !== null) {
                    row[vehicleId] = updatedDevice.vehicleId
                }
                updatedDevice.imei?.let { row[imei] = it }
                updatedDevice.latitude?.let { row[latitude] = it.toBigDecimal() }
                updatedDevice.longitude?.let { row[longitude] = it.toBigDecimal() }
                updatedDevice.speed?.let { row[speed] = it.toBigDecimal() }
                updatedDevice.heading?.let { row[heading] = it.toBigDecimal() }
                updatedDevice.dateTime?.let { row[dateTime] = it }
                updatedDevice.iconMapUrl?.let { row[iconMapUrl] = it }
                updatedDevice.title?.let { row[title] = it }
                updatedDevice.ignition?.let { row[ignition] = it }
            }
        }

        return ServiceResponse(HttpStatusCode.OK, Message("Dispositivo GPS atualizado com sucesso"))
    }

    suspend fun deleteGpsDevice(id: Int): ServiceResponse<Message> {
        val existingDevice = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()
        }

        if (existingDevice == null) {
            return ServiceResponse(HttpStatusCode.NotFound, Message("Dispositivo GPS não encontrado!"))
        }

        // Verifica se o dispositivo está vinculado a um veículo
        val vehicleId = existingDevice[GpsDevicesTable.vehicleId]
        if (vehicleId != null) {
            return ServiceResponse(
                HttpStatusCode.BadRequest,
                Message("Não é possível deletar um dispositivo vinculado a um veículo. Desvincule-o primeiro.")
            )
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.deleteWhere { GpsDevicesTable.id eq id }
        }

        return ServiceResponse(HttpStatusCode.OK, Message("Dispositivo GPS deletado com sucesso"))
    }

    suspend fun getAllGpsDevices(
        page: Int = 1,
        limit: Int = 10,
        vehicleIdFilter: Int? = null,
        imeiFilter: String? = null
    ): ServiceResponse<PaginatedResponse<GpsDevice>> {
        return DatabaseFactory.dbQuery {
            val query = GpsDevicesTable.selectAll().apply {
                vehicleIdFilter?.let { andWhere { GpsDevicesTable.vehicleId eq it } }
                imeiFilter?.let { andWhere { GpsDevicesTable.imei eq it } }
            }

            val total = query.count()

            val results = query
                .orderBy(GpsDevicesTable.id to SortOrder.DESC)
                .limit(limit)
                .offset(start = ((page - 1) * limit).toLong())
                .map { row ->
                    GpsDevice(
                        id = row[GpsDevicesTable.id],
                        vehicleId = row[GpsDevicesTable.vehicleId], // CORRIGIDO: pode ser null
                        imei = row[GpsDevicesTable.imei],
                        latitude = row[GpsDevicesTable.latitude].toDouble(),
                        longitude = row[GpsDevicesTable.longitude].toDouble(),
                        dateTime = row[GpsDevicesTable.dateTime],
                        speed = row[GpsDevicesTable.speed].toDouble(),
                        heading = row[GpsDevicesTable.heading].toDouble(),
                        iconMapUrl = row[GpsDevicesTable.iconMapUrl],
                        title = row[GpsDevicesTable.title],
                        ignition = row[GpsDevicesTable.ignition]
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

    suspend fun findGpsDeviceById(id: Int): ServiceResponse<Any> {
        val device = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()?.let { row ->
                GpsDevice(
                    id = row[GpsDevicesTable.id],
                    vehicleId = row[GpsDevicesTable.vehicleId], // CORRIGIDO: pode ser null
                    imei = row[GpsDevicesTable.imei],
                    latitude = row[GpsDevicesTable.latitude].toDouble(),
                    longitude = row[GpsDevicesTable.longitude].toDouble(),
                    dateTime = row[GpsDevicesTable.dateTime],
                    speed = row[GpsDevicesTable.speed].toDouble(),
                    heading = row[GpsDevicesTable.heading].toDouble(),
                    iconMapUrl = row[GpsDevicesTable.iconMapUrl],
                    title = row[GpsDevicesTable.title],
                    ignition = row[GpsDevicesTable.ignition]
                )
            }
        }

        return if (device == null) {
            ServiceResponse(HttpStatusCode.NotFound, mapOf("message" to "Dispositivo GPS não encontrado"))
        } else {
            ServiceResponse(HttpStatusCode.OK, device)
        }
    }

    suspend fun findGpsDeviceByVehicleId(vehicleId: Int): ServiceResponse<Any> {
        val device = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.vehicleId eq vehicleId }.singleOrNull()?.let { row ->
                GpsDevice(
                    id = row[GpsDevicesTable.id],
                    vehicleId = row[GpsDevicesTable.vehicleId],
                    imei = row[GpsDevicesTable.imei],
                    latitude = row[GpsDevicesTable.latitude].toDouble(),
                    longitude = row[GpsDevicesTable.longitude].toDouble(),
                    dateTime = row[GpsDevicesTable.dateTime],
                    speed = row[GpsDevicesTable.speed].toDouble(),
                    heading = row[GpsDevicesTable.heading].toDouble(),
                    iconMapUrl = row[GpsDevicesTable.iconMapUrl],
                    title = row[GpsDevicesTable.title],
                    ignition = row[GpsDevicesTable.ignition]
                )
            }
        }

        return if (device == null) {
            ServiceResponse(HttpStatusCode.NotFound, mapOf("message" to "Dispositivo GPS não encontrado para o veículo informado"))
        } else {
            ServiceResponse(HttpStatusCode.OK, device)
        }
    }

    suspend fun getHistoryByVehicle(
        vehicleId: Int,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): ServiceResponse<List<GpsHistory>> {
        return DatabaseFactory.dbQuery {
            // Define o fuso horário
            val tz = TimeZone.currentSystemDefault()

            // Início e fim do dia atual caso start/end sejam nulos
            val today = kotlinx.datetime.Clock.System.todayIn(tz)
            val todayStart = startDate ?: today.atStartOfDayIn(tz).toLocalDateTime(tz)
            val todayEnd = endDate ?: today.atTime(23, 59, 59, 999_999_999)

            // Cria a query filtrando veículo e intervalo de tempo
            val query = GpsHistoryTable.selectAll().where {
                (GpsHistoryTable.vehicleId eq vehicleId) and
                        (GpsHistoryTable.dateTime greaterEq todayStart) and
                        (GpsHistoryTable.dateTime lessEq todayEnd)
            }

            // Ordena pelo timestamp
            val results = query
                .orderBy(GpsHistoryTable.dateTime to SortOrder.ASC)
                .map { row ->
                    GpsHistory(
                        id = row[GpsHistoryTable.id],
                        gpsDeviceId = row[GpsHistoryTable.gpsDeviceId],
                        vehicleId = row[GpsHistoryTable.vehicleId],
                        dateTime = row[GpsHistoryTable.dateTime],
                        latitude = row[GpsHistoryTable.latitude].toDouble(),
                        longitude = row[GpsHistoryTable.longitude].toDouble(),
                        rawLog = row[GpsHistoryTable.rawLog],
                    )
                }

            ServiceResponse(HttpStatusCode.OK, results)
        }
    }

}