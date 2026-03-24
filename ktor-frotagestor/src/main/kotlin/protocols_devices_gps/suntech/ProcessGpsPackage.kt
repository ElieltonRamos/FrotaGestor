package com.frotagestor.protocols_devices_gps.suntech

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import com.frotagestor.services.AutoTripService
import com.frotagestor.services.DailyTripService
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

suspend fun findVehicleIdByImei(imei: String): Int? {
    return DatabaseFactory.dbQuery {
        GpsDevicesTable
            .selectAll()
            .where { GpsDevicesTable.imei eq imei }
            .map { it[GpsDevicesTable.vehicleId] }
            .singleOrNull()
    }
}

private val autoTripService = AutoTripService()
private val dailyTripService = DailyTripService()

enum class GpsQuality {
    EXCELLENT,  // 8+ satélites, GPS fixed
    GOOD,       // 5-7 satélites, GPS fixed
    FAIR,       // 3-4 satélites, GPS fixed
    POOR,       // < 3 satélites ou sem fix
    NO_SIGNAL   // Sem GPS
}

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val ignition: Boolean,
    val deviceDateTime: LocalDateTime,
    val serverDateTime: LocalDateTime,

    val messageType: String,
    val deviceId: String,
    val swVersion: String?,
    val cellId: String?,
    val satellites: Int?,
    val gpsFixed: Boolean,
    val gpsQuality: GpsQuality,
    val odometer: Long?,
    val batteryVoltage: Double?,
    val statusBits: String?,
    val eventCode: Int?,
    val mode: Int?,
    val messageNumber: String?,
    val ioStatus: Int?
) {
    fun toCompactLog(): String = buildString {
        append("type=").append(messageType)
        append(" | device=").append(deviceId)
        append(" | devTime=").append(deviceDateTime)
        append(" | srvTime=").append(serverDateTime)

        append(" | lat=").append(latitude.format(6))
        append(" | lon=").append(longitude.format(6))
        append(" | speed=").append(speed)
        append(" | heading=").append(heading)

        append(" | ign=").append(ignition)
        append(" | sats=").append(satellites)
        append(" | gps=").append(gpsQuality)

        append(" | batt=").append(batteryVoltage)
        append(" | odo=").append(odometer)

        append(" | sw=").append(swVersion)
        append(" | cell=").append(cellId)
        append(" | statusBits=").append(statusBits)

        append(" | event=").append(eventCode)
        append(" | mode=").append(mode)
        append(" | msgNum=").append(messageNumber)
        append(" | io=").append(ioStatus)
    }
}

suspend fun processMessage(
    msg: String,
    currentId: String?,
    onAck: suspend (String, String) -> Unit
) {
    val timestamp = generateDate()

    when {
        msg.startsWith("ST300ALV;") -> {
            val id = msg.substringAfter("ST300ALV;").substringBefore(";").trim()
            println("[$timestamp] ❤️ HEARTBEAT | $id")
            updateDeviceLastSeen(id)
            onAck("ST300ALV", id)
        }

        msg.startsWith("ST300GPS;") ||
                msg.startsWith("ST300STT;") ||
                msg.startsWith("ST300ALT;") ||
                msg.startsWith("ST300EMG;") ||
                msg.startsWith("ST300EVT;") -> {

            val imei = currentId ?: extractDeviceId(msg)
            if (imei == null) {
                println("[$timestamp] ❌ IMEI não encontrado: ${msg.take(50)}...")
                return
            }

            val gps = parseGpsPacket(msg)
            if (gps == null) {
                println("[$timestamp] ❌ Parse falhou: ${msg.take(50)}...")
                return
            }

            println("[$timestamp] ${gps.toCompactLog()}")
            saveOrUpdateGps(imei, gps, msg)
        }

        else -> {
            println("[$timestamp] ⚠️ Pacote desconhecido: ${msg.take(80)}...")
        }
    }
}

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

        // Atualiza posição atual do dispositivo
        GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) {
            it[latitude] = gps.latitude.toBigDecimal()
            it[longitude] = gps.longitude.toBigDecimal()
            it[speed] = gps.speed.toBigDecimal()
            it[heading] = (gps.heading % 360).toBigDecimal()
            it[dateTime] = gps.serverDateTime
            it[ignition] = gps.ignition
            it[lastCommunication] = gps.serverDateTime
            it[batteryVoltage] = gps.batteryVoltage?.toBigDecimal()
        }

        // Salva no histórico com TODOS os dados
        GpsHistoryTable.insert {
            it[GpsHistoryTable.gpsDeviceId] = gpsDeviceId
            it[GpsHistoryTable.vehicleId] = vehicleId
            it[GpsHistoryTable.latitude] = gps.latitude.toBigDecimal()
            it[GpsHistoryTable.longitude] = gps.longitude.toBigDecimal()
            it[GpsHistoryTable.speed] = gps.speed.toBigDecimal()
            it[GpsHistoryTable.heading] = gps.heading.toBigDecimal()
            it[GpsHistoryTable.dateTime] = gps.deviceDateTime
            it[GpsHistoryTable.ignition] = gps.ignition
            it[GpsHistoryTable.satellites] = gps.satellites
            it[GpsHistoryTable.gpsFixed] = gps.gpsFixed
            it[GpsHistoryTable.odometer] = gps.odometer
            it[GpsHistoryTable.batteryVoltage] = gps.batteryVoltage?.toBigDecimal()
            it[GpsHistoryTable.messageType] = gps.messageType
            it[GpsHistoryTable.eventCode] = gps.eventCode
            it[GpsHistoryTable.gpsQuality] = gps.gpsQuality.name
            it[GpsHistoryTable.rawLog] = rawMessage
        }
    }
    dailyTripService.processGpsData(
        imei = imei,
        vehicleId = vehicleId,
        latitude = gps.latitude,
        longitude = gps.longitude,
        dateTime = gps.serverDateTime,
        speed = gps.speed,
        ignition = gps.ignition,
        odometer = gps.odometer
    )
//
//    autoTripService.processIgnitionChange(
//        imei = imei,
//        vehicleId = vehicleId,
//        latitude = gps.latitude,
//        longitude = gps.longitude,
//        ignition = gps.ignition,
//        dateTime = gps.serverDateTime,
//        speed = gps.speed
//    )
//
//    autoTripService.checkPendingTimers(
//        vehicleId = vehicleId,
//        latitude = gps.latitude,
//        longitude = gps.longitude,
//        currentTime = gps.serverDateTime,
//        speed = gps.speed
//    )
//
//    autoTripService.updateActiveTrip(
//        imei = imei,
//        vehicleId = vehicleId,
//        latitude = gps.latitude,
//        longitude = gps.longitude,
//        dateTime = gps.serverDateTime,
//        speed = gps.speed
//    )
}

suspend fun updateDeviceLastSeen(imei: String) {
    DatabaseFactory.dbQuery {
        GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) {
            it[lastCommunication] = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
}

fun extractDeviceId(message: String): String? = try {
    message.split(";").getOrNull(1)?.trim()
} catch (_: Exception) {
    null
}

fun parseGpsPacket(data: String): GpsData? = try {
    val parts = data.split(";")

    val messageType = parts.getOrNull(0) ?: return null
    val deviceId = parts.getOrNull(1) ?: return null

    val latitude = parts.getOrNull(7)?.toDoubleOrNull()?.let {
        if (it in -90.0..90.0) it else {
            println("❌ Latitude inválida: $it")
            return null
        }
    } ?: return null

    val longitude = parts.getOrNull(8)?.toDoubleOrNull()?.let {
        if (it in -180.0..180.0) it else {
            println("❌ Longitude inválida: $it")
            return null
        }
    } ?: return null

    val speed = parts.getOrNull(9)?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val heading = parts.getOrNull(10)?.toDoubleOrNull()?.let { (it % 360).coerceIn(0.0, 360.0) } ?: 0.0
    val satellites = parts.getOrNull(11)?.toIntOrNull()?.coerceIn(0, 50)
    val gpsFixValue = parts.getOrNull(12)?.toIntOrNull() ?: 0
    val gpsFixed = gpsFixValue == 1
    val odometer = parts.getOrNull(13)?.toLongOrNull()?.coerceAtLeast(0)
    val batteryVoltage = parts.getOrNull(14)?.toDoubleOrNull()?.coerceIn(0.0, 30.0)

    val swVersion = parts.getOrNull(2)
    val cellId = parts.getOrNull(6)
    val statusBits = parts.getOrNull(15)
    val eventCode = parts.getOrNull(16)?.toIntOrNull()
    val mode = parts.getOrNull(17)?.toIntOrNull()
    val messageNumber = if (messageType.contains("STT")) parts.getOrNull(18) else null
    val ioStatus = parts.lastOrNull()?.toIntOrNull()

    val ignition = validateIgnition(statusBits)

    val gpsQuality = calculateGpsQuality(satellites, gpsFixed)

    val deviceDateTime = parseDeviceDateTime(parts.getOrNull(4), parts.getOrNull(5))
        ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    val serverDateTime = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    GpsData(
        latitude = latitude,
        longitude = longitude,
        speed = speed,
        heading = heading,
        ignition = ignition,
        deviceDateTime = deviceDateTime,
        serverDateTime = serverDateTime,
        messageType = messageType,
        deviceId = deviceId,
        swVersion = swVersion,
        cellId = cellId,
        satellites = satellites,
        gpsFixed = gpsFixed,
        gpsQuality = gpsQuality,
        odometer = odometer,
        batteryVoltage = batteryVoltage,
        statusBits = statusBits,
        eventCode = eventCode,
        mode = mode,
        messageNumber = messageNumber,
        ioStatus = ioStatus
    )

} catch (e: Exception) {
    println("❌ Erro parse GPS: ${e.message}")
    null
}

fun validateIgnition(statusBits: String?): Boolean {
    return when {
        statusBits.isNullOrBlank() -> false
        statusBits.length != 6 -> {
            println("⚠️ StatusBits tamanho inválido: '$statusBits'")
            false
        }
        !statusBits.all { it in '0'..'1' } -> {
            println("⚠️ StatusBits com caracteres inválidos: '$statusBits'")
            false
        }
        statusBits[0] == '1' -> true  // Bit 0 = ignição
        else -> false
    }
}

fun calculateGpsQuality(satellites: Int?, gpsFixed: Boolean): GpsQuality {
    return when {
        !gpsFixed -> GpsQuality.NO_SIGNAL
        satellites == null -> GpsQuality.POOR
        satellites >= 8 -> GpsQuality.EXCELLENT
        satellites >= 5 -> GpsQuality.GOOD
        satellites >= 3 -> GpsQuality.FAIR
        else -> GpsQuality.POOR
    }
}

fun parseDeviceDateTime(dateStr: String?, timeStr: String?): LocalDateTime? {
    return try {
        if (dateStr == null || timeStr == null) return null
        if (dateStr.length != 8) return null

        val year = dateStr.substring(0, 4).toInt()
        val month = dateStr.substring(4, 6).toInt()
        val day = dateStr.substring(6, 8).toInt()

        if (month !in 1..12 || day !in 1..31) return null

        val timeParts = timeStr.split(":")
        if (timeParts.size != 3) return null

        val (h, m, s) = timeParts.map { it.toInt() }
        if (h !in 0..23 || m !in 0..59 || s !in 0..59) return null

        val utc = LocalDateTime(year, month, day, h, m, s)
        utc.toInstant(TimeZone.UTC)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    } catch (e: Exception) {
        null
    }
}

fun generateDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d:%02d".format(now.hour, now.minute, now.second)
}

fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)