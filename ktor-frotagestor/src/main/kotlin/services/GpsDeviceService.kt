package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateGpsDevice
import com.frotagestor.validations.validatePartialGpsDevice
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import kotlin.let

class GpsDeviceService {

    suspend fun createGpsDevice(req: String): ServiceResponse<Message> {
        val newDevice = validateGpsDevice(req).getOrReturn { msg ->
            return ServiceResponse(HttpStatusCode.BadRequest, Message(msg))
        }

        val existingDevice = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll()
                .where { GpsDevicesTable.imei eq newDevice.imei }
                .singleOrNull()
        }

        if (existingDevice != null) {
            return ServiceResponse(HttpStatusCode.Conflict, Message("Dispositivo já cadastrado!"))
        }

        DatabaseFactory.dbQuery {
            GpsDevicesTable.insert {
                it[vehicleId] = newDevice.vehicleId
                it[imei] = newDevice.imei
                it[latitude] = (newDevice.latitude).toBigDecimal()
                it[longitude] = (newDevice.longitude).toBigDecimal()
                it[speed] = (newDevice.speed).toBigDecimal()
                it[heading] = (newDevice.heading).toBigDecimal()
                it[dateTime] = newDevice.dateTime
                it[iconMapUrl] = newDevice.iconMapUrl
                it[title] = newDevice.title
                it[ignition] = newDevice.ignition
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
        vehicleIdFilter: Int? = null
    ): ServiceResponse<PaginatedResponse<GpsDevice>> {
        return DatabaseFactory.dbQuery {
            val query = GpsDevicesTable.selectAll().apply {
                vehicleIdFilter?.let { andWhere { GpsDevicesTable.vehicleId eq it } }
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
}
