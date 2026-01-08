package com.frotagestor.protocols_devices_gps.suntech

import com.frotagestor.accurate_gt_06.findVehicleIdByImei
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import com.frotagestor.services.AutoTripService
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

// =======================
// INSTÂNCIA GLOBAL
// =======================
private val autoTripService = AutoTripService()

// =======================
// MODELO GPS
// =======================
data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val ignition: Boolean,
    val dateTime: LocalDateTime,
)

// =======================
// PROCESSAMENTO TCP
// =======================
suspend fun processMessage(
    msg: String,
    currentId: String?,
    onAck: suspend (String, String) -> Unit
) {
    when {
        msg.startsWith("ST300ALV;") -> {
            val id = msg.substringAfter("ST300ALV;").substringBefore(";").trim()
            println("[${generateDate()}] ❤️ HEARTBEAT – ID: $id")
        }

        msg.startsWith("ST300GPS;") ||
                msg.startsWith("ST300STT;") ||
                msg.startsWith("ST300ALT;") ||
                msg.startsWith("ST300EMG;") ||
                msg.startsWith("ST300EVT;") -> {

            val imei = currentId ?: extractDeviceId(msg) ?: return
            val gps = parseGpsPacket(msg) ?: return

            saveOrUpdateGps(imei, gps, msg)
        }

        else -> {
            println("[${generateDate()}] ⚠️ Pacote desconhecido: $msg")
        }
    }
}

// =======================
// SALVA GPS + VIAGEM
// =======================
suspend fun saveOrUpdateGps(
    imei: String,
    gps: GpsData,
    rawMessage: String = ""
) {
    val vehicleId = findVehicleIdByImei(imei)
    if (vehicleId == null) {
        println("⚠️ IMEI $imei não vinculado a veículo")
        return
    }

    DatabaseFactory.dbQuery {
        val device = GpsDevicesTable
            .selectAll()
            .where { GpsDevicesTable.imei eq imei }
            .singleOrNull() ?: return@dbQuery

        val gpsDeviceId = device[GpsDevicesTable.id]

        // Atualiza dispositivo
        GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) {
            it[latitude] = gps.latitude.toBigDecimal()
            it[longitude] = gps.longitude.toBigDecimal()
            it[speed] = gps.speed.toBigDecimal()
            it[heading] = (gps.heading % 360).toBigDecimal()
            it[dateTime] = gps.dateTime
            it[ignition] = gps.ignition
        }

        // Histórico
        GpsHistoryTable.insert {
            it[GpsHistoryTable.gpsDeviceId] = gpsDeviceId
            it[GpsHistoryTable.vehicleId] = vehicleId
            it[GpsHistoryTable.latitude] = gps.latitude.toBigDecimal()
            it[GpsHistoryTable.longitude] = gps.longitude.toBigDecimal()
            it[GpsHistoryTable.speed] = gps.speed.toBigDecimal()
            it[GpsHistoryTable.dateTime] = gps.dateTime
            it[GpsHistoryTable.rawLog] = rawMessage
        }
    }

    // =======================
    // INTEGRAÇÃO COM VIAGENS
    // =======================
    autoTripService.processIgnitionChange(
        imei = imei,
        vehicleId = vehicleId,
        latitude = gps.latitude,
        longitude = gps.longitude,
        ignition = gps.ignition,
        dateTime = gps.dateTime,
        speed = gps.speed
    )

    autoTripService.updateActiveTrip(
        imei = imei,
        vehicleId = vehicleId,
        latitude = gps.latitude,
        longitude = gps.longitude,
        dateTime = gps.dateTime,
        speed = gps.speed
    )
}

// =======================
// PARSERS
// =======================
fun extractDeviceId(message: String): String? = try {
    message.split(";").getOrNull(1)?.trim()
} catch (_: Exception) {
    null
}

fun parseGpsPacket(data: String): GpsData? = try {
    val parts = data.split(";")

    val latitude = parts.getOrNull(7)?.toDoubleOrNull() ?: return null
    val longitude = parts.getOrNull(8)?.toDoubleOrNull() ?: return null
    val speed = parts.getOrNull(9)?.toDoubleOrNull() ?: 0.0
    val heading = parts.getOrNull(10)?.toDoubleOrNull() ?: 0.0
    val ioStatus = parts.lastOrNull()?.toIntOrNull() ?: 0

    val eventCode = parts.getOrNull(16)?.toIntOrNull()
    val ignition = when (eventCode) {
        40 -> true
        41 -> false
        else -> ioStatus == 1
    }

    val dateStr = parts.getOrNull(4)
    val timeStr = parts.getOrNull(5)
    val dateTime = if (dateStr != null && timeStr != null) {
        parseDeviceDateTime(dateStr, timeStr)
    } else {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    GpsData(latitude, longitude, speed, heading, ignition, dateTime)

} catch (e: Exception) {
    println("❌ Erro parse GPS: ${e.message}")
    null
}

// =======================
// DATA
// =======================
fun parseDeviceDateTime(dateStr: String, timeStr: String): LocalDateTime {
    val year = dateStr.substring(0, 4).toInt()
    val month = dateStr.substring(4, 6).toInt()
    val day = dateStr.substring(6, 8).toInt()
    val (h, m, s) = timeStr.split(":").map { it.toInt() }

    val utc = LocalDateTime(year, month, day, h, m, s)
    return utc.toInstant(TimeZone.UTC)
        .toLocalDateTime(TimeZone.currentSystemDefault())
}

fun generateDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d %02d:%02d:%02d".format(
        now.year, now.monthNumber, now.dayOfMonth,
        now.hour, now.minute, now.second
    )
}
