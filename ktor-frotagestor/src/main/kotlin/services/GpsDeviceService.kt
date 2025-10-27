package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateGpsDevice
import com.frotagestor.validations.validatePartialGpsDevice
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import kotlin.let

class GpsDeviceService {

    suspend fun createGpsDevice(req: String): ServiceResponse<Message> {
        val newDevice = validateGpsDevice(req).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, Message(msg))
        }
        if (newDevice.imei.isNullOrBlank()) {
            return ServiceResponse(HttpStatusCode.BadRequest, Message("O campo IMEI é obrigatório"))
        }
        val existingDevice = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().where { GpsDevicesTable.imei eq newDevice.imei!! }.singleOrNull()
        }
        if (existingDevice != null) {
            return ServiceResponse(HttpStatusCode.Conflict, Message("Dispositivo já cadastrado!"))
        }
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
        val deviceWithDefaults = newDevice.copy(
            vehicleId = newDevice.vehicleId ?: 0,
            latitude = newDevice.latitude ?: 0.0,
            longitude = newDevice.longitude ?: 0.0,
            speed = newDevice.speed ?: 0.0,
            heading = newDevice.heading ?: 0.0,
            dateTime = newDevice.dateTime ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            iconMapUrl = newDevice.iconMapUrl ?: null,
            title = newDevice.title ?: null,
            ignition = newDevice.ignition ?: false
        )

        DatabaseFactory.dbQuery {
            GpsDevicesTable.insert { row ->
                deviceWithDefaults.vehicleId?.let { row[vehicleId] = it }
                deviceWithDefaults.imei?.let { row[imei] = it }
                deviceWithDefaults.latitude?.let { row[latitude] = it.toBigDecimal() }
                deviceWithDefaults.longitude?.let { row[longitude] = it.toBigDecimal() }
                deviceWithDefaults.speed?.let { row[speed] = it.toBigDecimal() }
                deviceWithDefaults.heading?.let { row[heading] = it.toBigDecimal() }
                deviceWithDefaults.dateTime?.let { row[dateTime] = it }
                deviceWithDefaults.iconMapUrl?.let { row[iconMapUrl] = it }
                deviceWithDefaults.title?.let { row[title] = it }
                deviceWithDefaults.ignition?.let { row[ignition] = it }
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

        DatabaseFactory.dbQuery {
            GpsDevicesTable.update({ GpsDevicesTable.id eq id }) { row ->
                updatedDevice.vehicleId?.let { v -> row[vehicleId] = v }
                updatedDevice.imei?.let { i -> row[imei] = i }
                updatedDevice.latitude?.let { lat -> row[latitude] = lat.toBigDecimal() }
                updatedDevice.longitude?.let { lon -> row[longitude] = lon.toBigDecimal() }
                updatedDevice.speed?.let { s -> row[speed] = s.toBigDecimal() }
                updatedDevice.heading?.let { h -> row[heading] = h.toBigDecimal() }
                updatedDevice.dateTime?.let { dt -> row[dateTime] = dt }
                updatedDevice.iconMapUrl?.let { url -> row[iconMapUrl] = url }
                updatedDevice.title?.let { t -> row[title] = t }
                updatedDevice.ignition?.let { ig -> row[ignition] = ig }
            }
        }

        return ServiceResponse(HttpStatusCode.OK, Message("Dispositivo GPS atualizado com sucesso"))
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
                .orderBy(GpsDevicesTable.dateTime to SortOrder.DESC)
                .limit(limit)
                .offset(start = ((page - 1) * limit).toLong())
                .map {
                    GpsDevice(
                        id = it[GpsDevicesTable.id],
                        vehicleId = it[GpsDevicesTable.vehicleId],
                        imei = it[GpsDevicesTable.imei],
                        latitude = it[GpsDevicesTable.latitude].toDouble(),
                        longitude = it[GpsDevicesTable.longitude].toDouble(),
                        dateTime = it[GpsDevicesTable.dateTime],
                        speed = it[GpsDevicesTable.speed].toDouble(),
                        heading = it[GpsDevicesTable.heading].toDouble(),
                        iconMapUrl = it[GpsDevicesTable.iconMapUrl],
                        title = it[GpsDevicesTable.title],
                        ignition = it[GpsDevicesTable.ignition]
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
            GpsDevicesTable.selectAll().where { GpsDevicesTable.id eq id }.singleOrNull()?.let {
                GpsDevice(
                    id = it[GpsDevicesTable.id],
                    vehicleId = it[GpsDevicesTable.vehicleId],
                    imei = it[GpsDevicesTable.imei],
                    latitude = it[GpsDevicesTable.latitude].toDouble(),
                    longitude = it[GpsDevicesTable.longitude].toDouble(),
                    dateTime = it[GpsDevicesTable.dateTime],
                    speed = it[GpsDevicesTable.speed].toDouble(),
                    heading = it[GpsDevicesTable.heading].toDouble(),
                    iconMapUrl = it[GpsDevicesTable.iconMapUrl],
                    title = it[GpsDevicesTable.title],
                    ignition = it[GpsDevicesTable.ignition]
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
            GpsDevicesTable.selectAll().where { GpsDevicesTable.vehicleId eq vehicleId }.singleOrNull()?.let {
                GpsDevice(
                    id = it[GpsDevicesTable.id],
                    vehicleId = it[GpsDevicesTable.vehicleId],
                    imei = it[GpsDevicesTable.imei],
                    latitude = it[GpsDevicesTable.latitude].toDouble(),
                    longitude = it[GpsDevicesTable.longitude].toDouble(),
                    dateTime = it[GpsDevicesTable.dateTime],
                    speed = it[GpsDevicesTable.speed].toDouble(),
                    heading = it[GpsDevicesTable.heading].toDouble(),
                    iconMapUrl = it[GpsDevicesTable.iconMapUrl],
                    title = it[GpsDevicesTable.title],
                    ignition = it[GpsDevicesTable.ignition]
                )
            }
        }

        return if (device == null) {
            ServiceResponse(HttpStatusCode.NotFound, mapOf("message" to "Dispositivo GPS não encontrado para o veículo informado"))
        } else {
            ServiceResponse(HttpStatusCode.OK, device)
        }
    }
}
