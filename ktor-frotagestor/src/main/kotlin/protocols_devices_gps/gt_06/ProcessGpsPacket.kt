package com.frotagestor.protocols_devices_gps.gt06

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
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

private val dailyTripService = DailyTripService()

// ─── Protocol Numbers ────────────────────────────────────────────────────────
object GT06Protocol {
    const val LOGIN       = 0x01
    const val GPS         = 0x10
    const val LBS         = 0x11
    const val GPS_LBS     = 0x12
    const val HEARTBEAT   = 0x13
    const val ALARM       = 0x16  // GPS + LBS + Status merged
    const val CMD_REPLY   = 0x15  // device → server (resposta de comando)
    const val CMD_SERVER  = 0x80  // server → device
}

// ─── Result de processamento ─────────────────────────────────────────────────
data class GT06ProcessResult(
    val imei: String? = null,
    val response: ByteArray? = null
)

enum class GpsQuality { EXCELLENT, GOOD, FAIR, POOR, NO_SIGNAL }

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
    val satellites: Int?,
    val gpsFixed: Boolean,
    val gpsQuality: GpsQuality,
    val mcc: Int?,
    val mnc: Int?,
    val lac: Int?,
    val cellId: Int?,
    val voltageLevel: Int?,
    val gsmSignal: Int?,
    val alarmType: Int?,
    val serialNumber: Int
) {
    fun toCompactLog(): String = buildString {
        append("type=").append(messageType)
        append(" | device=").append(deviceId)
        append(" | devTime=").append(deviceDateTime)
        append(" | lat=").append(latitude.format(6))
        append(" | lon=").append(longitude.format(6))
        append(" | speed=").append(speed)
        append(" | heading=").append(heading)
        append(" | ign=").append(ignition)
        append(" | sats=").append(satellites)
        append(" | gps=").append(gpsQuality)
        append(" | volt=").append(voltageLevel)
        append(" | gsm=").append(gsmSignal)
        append(" | alarm=").append(alarmType)
        append(" | sn=").append(serialNumber)
    }
}

// ─── Entry point ─────────────────────────────────────────────────────────────
suspend fun processPacketGT06(packet: ByteArray, currentImei: String?): GT06ProcessResult {
    if (packet.size < 5) return GT06ProcessResult()

    val protocolNo = packet[3].toInt() and 0xFF
    // Conteúdo: packet[4] até packet[size-5] (exclui start, length, protocolNo, serialNo(2), crc(2), stop(2))
    val contentEnd = packet.size - 5
    val content = if (contentEnd > 4) packet.copyOfRange(4, contentEnd) else ByteArray(0)
    val serialNumber = ((packet[packet.size - 5].toInt() and 0xFF) shl 8) or (packet[packet.size - 4].toInt() and 0xFF)

    return when (protocolNo) {
        GT06Protocol.LOGIN -> handleLogin(packet, content, serialNumber)
        GT06Protocol.HEARTBEAT -> handleHeartbeat(content, serialNumber, currentImei)
        GT06Protocol.GPS -> handleGps(content, serialNumber, currentImei, "GPS")
        GT06Protocol.GPS_LBS -> handleGps(content, serialNumber, currentImei, "GPS_LBS")
        GT06Protocol.ALARM -> handleAlarm(content, serialNumber, currentImei)
        GT06Protocol.CMD_REPLY -> handleCommandReply(content, currentImei)
        else -> {
            println("[${generateDate()}] ⚠️ Protocol desconhecido: 0x${"%02X".format(protocolNo)}")
            GT06ProcessResult()
        }
    }
}

// ─── LOGIN (0x01) ─────────────────────────────────────────────────────────────
// Content: 8 bytes IMEI (BCD, sem o primeiro nibble)
private fun handleLogin(packet: ByteArray, content: ByteArray, serialNumber: Int): GT06ProcessResult {
    if (content.size < 8) {
        println("[${generateDate()}] ❌ Login packet muito curto")
        return GT06ProcessResult()
    }

    val imei = decodeBcdImei(content.copyOfRange(0, 8))
    println("[${generateDate()}] 🔑 LOGIN | IMEI=$imei")

    val response = buildResponse(GT06Protocol.LOGIN, serialNumber)
    return GT06ProcessResult(imei = imei, response = response)
}

// ─── HEARTBEAT (0x13) ────────────────────────────────────────────────────────
// Content: terminalInfo(1) + voltageLevel(1) + gsmSignal(1) + alarm/lang(2)
private suspend fun handleHeartbeat(content: ByteArray, serialNumber: Int, imei: String?): GT06ProcessResult {
    val voltageLevel = content.getOrNull(1)?.toInt()?.and(0xFF)
    val gsmSignal = content.getOrNull(2)?.toInt()?.and(0xFF)

    println("[${generateDate()}] ❤️ HEARTBEAT | IMEI=$imei | volt=$voltageLevel | gsm=$gsmSignal")

    if (imei != null) updateDeviceLastSeen(imei)

    val response = buildResponse(GT06Protocol.HEARTBEAT, serialNumber)
    return GT06ProcessResult(response = response)
}

// ─── GPS (0x10) / GPS+LBS (0x12) ─────────────────────────────────────────────
private suspend fun handleGps(
    content: ByteArray,
    serialNumber: Int,
    imei: String?,
    type: String
): GT06ProcessResult {
    if (imei == null) {
        println("[${generateDate()}] ❌ GPS recebido sem IMEI registrado")
        return GT06ProcessResult()
    }

    val gps = parseGpsContent(content, imei, type, serialNumber) ?: return GT06ProcessResult()
    println("[${generateDate()}] ${gps.toCompactLog()}")

    saveOrUpdateGps(imei, gps)
    return GT06ProcessResult()
}

// ─── ALARM (0x16) ────────────────────────────────────────────────────────────
// Estrutura igual ao GPS+LBS com campos adicionais de status/alarme
private suspend fun handleAlarm(content: ByteArray, serialNumber: Int, imei: String?): GT06ProcessResult {
    if (imei == null) {
        println("[${generateDate()}] ❌ ALARM recebido sem IMEI registrado")
        return GT06ProcessResult()
    }

    val gps = parseGpsContent(content, imei, "ALARM", serialNumber) ?: return GT06ProcessResult()
    println("[${generateDate()}] 🚨 ALARM | ${gps.toCompactLog()}")

    saveOrUpdateGps(imei, gps)

    // Responde ao alarme com o mesmo formato padrão
    val response = buildResponse(GT06Protocol.ALARM, serialNumber)
    return GT06ProcessResult(response = response)
}

// ─── CMD_REPLY (0x15) ────────────────────────────────────────────────────────
private fun handleCommandReply(content: ByteArray, imei: String?): GT06ProcessResult {
    val text = content.toString(Charsets.US_ASCII).trim()
    println("[${generateDate()}] 📨 CMD REPLY | IMEI=$imei | $text")
    return GT06ProcessResult()
}

// ─── Parser de conteúdo GPS ──────────────────────────────────────────────────
/**
 * Layout do GPS content (0x10 / 0x12 / 0x16):
 * [0]    Year   (1 byte)
 * [1]    Month  (1 byte)
 * [2]    Day    (1 byte)
 * [3]    Hour   (1 byte)
 * [4]    Minute (1 byte)
 * [5]    Second (1 byte)
 * [6]    GPS info length + satellites (1 byte: high nibble = length, low nibble = sats)
 * [7-10] Latitude  (4 bytes, uint32, divide por 1_800_000.0)
 * [11-14]Longitude (4 bytes, uint32, divide por 1_800_000.0)
 * [15]   Speed (1 byte, km/h)
 * [16-17]Course + Status flags (2 bytes)
 *          Byte1 bit4: GPS fixed
 *          Byte1 bit3: East=0, West=1
 *          Byte1 bit2: South=0, North=1 (alguns inversem — validar com device real)
 *          Byte2 bits 0-6: heading (degrees, continued from byte1 bit0-6)
 * --- LBS (presente em 0x12 e 0x16) ---
 * [18-19] MCC (2 bytes)
 * [20]    MNC (1 byte)
 * [21-22] LAC (2 bytes)
 * [23-25] Cell ID (3 bytes)
 * --- Status extra (0x16 / ALARM) ---
 * [26]    Terminal Info (1 byte) — ignição no bit 1
 * [27]    Voltage Level (1 byte)
 * [28]    GSM Signal    (1 byte)
 * [29-30] Alarm + Language (2 bytes)
 */
fun parseGpsContent(content: ByteArray, imei: String, type: String, serialNumber: Int): GpsData? {
    return try {
        if (content.size < 18) {
            println("❌ GPS content muito curto: ${content.size} bytes")
            return null
        }

        val year   = 2000 + (content[0].toInt() and 0xFF)
        val month  = content[1].toInt() and 0xFF
        val day    = content[2].toInt() and 0xFF
        val hour   = content[3].toInt() and 0xFF
        val minute = content[4].toInt() and 0xFF
        val second = content[5].toInt() and 0xFF

        if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59 || second !in 0..59) {
            println("❌ Data/hora inválida no pacote GPS")
            return null
        }

        val gpsInfoByte = content[6].toInt() and 0xFF
        val satellites = gpsInfoByte and 0x0F  // low nibble

        val latRaw  = readUInt32(content, 7)
        val lonRaw  = readUInt32(content, 11)

        var latitude  = latRaw / 1_800_000.0
        var longitude = lonRaw / 1_800_000.0

        val courseByte1 = content[16].toInt() and 0xFF
        val courseByte2 = content[17].toInt() and 0xFF

        val gpsFixed  = (courseByte1 shr 4) and 0x01 == 1
        val isWest    = (courseByte1 shr 3) and 0x01 == 1
        val isSouth   = (courseByte1 shr 2) and 0x01 == 1

        if (isWest)  longitude = -longitude
        if (isSouth) latitude  = -latitude

        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            println("❌ Coordenadas fora dos limites: lat=$latitude lon=$longitude")
            return null
        }

        val speed   = (content[15].toInt() and 0xFF).toDouble()
        val heading = (((courseByte1 and 0x03) shl 8) or courseByte2).toDouble()

        // LBS (offset 18+)
        val mcc    = if (content.size > 19) readUInt16(content, 18) else null
        val mnc    = if (content.size > 20) content[20].toInt() and 0xFF else null
        val lac    = if (content.size > 22) readUInt16(content, 21) else null
        val cellId = if (content.size > 25) readUInt24(content, 23) else null

        // Status extra (offset 26+ — ALARM/0x16)
        val terminalInfo  = if (content.size > 26) content[26].toInt() and 0xFF else null
        val voltageLevel  = if (content.size > 27) content[27].toInt() and 0xFF else null
        val gsmSignal     = if (content.size > 28) content[28].toInt() and 0xFF else null
        val alarmByte     = if (content.size > 29) content[29].toInt() and 0xFF else null

        // Ignição: bit 1 do terminalInfo (quando disponível), fallback para gpsFixed
        val ignition = if (terminalInfo != null) {
            (terminalInfo shr 1) and 0x01 == 1
        } else {
            false
        }

        val gpsQuality = calculateGpsQuality(satellites, gpsFixed)

        val deviceDateTime = try {
            val utc = LocalDateTime(year, month, day, hour, minute, second)
            utc.toInstant(TimeZone.UTC).toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        GpsData(
            latitude = latitude,
            longitude = longitude,
            speed = speed,
            heading = heading % 360,
            ignition = ignition,
            deviceDateTime = deviceDateTime,
            serverDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            messageType = type,
            deviceId = imei,
            satellites = satellites,
            gpsFixed = gpsFixed,
            gpsQuality = gpsQuality,
            mcc = mcc,
            mnc = mnc,
            lac = lac,
            cellId = cellId,
            voltageLevel = voltageLevel,
            gsmSignal = gsmSignal,
            alarmType = alarmByte,
            serialNumber = serialNumber
        )
    } catch (e: Exception) {
        println("❌ Erro parseGpsContent: ${e.message}")
        null
    }
}

// ─── DB ──────────────────────────────────────────────────────────────────────
suspend fun saveOrUpdateGps(imei: String, gps: GpsData) {
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

        GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) {
            it[latitude]          = gps.latitude.toBigDecimal()
            it[longitude]         = gps.longitude.toBigDecimal()
            it[speed]             = gps.speed.toBigDecimal()
            it[heading]           = (gps.heading % 360).toBigDecimal()
            it[dateTime]          = gps.serverDateTime
            it[ignition]          = gps.ignition
            it[lastCommunication] = gps.serverDateTime
        }

        GpsHistoryTable.insert {
            it[GpsHistoryTable.gpsDeviceId]  = gpsDeviceId
            it[GpsHistoryTable.vehicleId]    = vehicleId
            it[GpsHistoryTable.latitude]     = gps.latitude.toBigDecimal()
            it[GpsHistoryTable.longitude]    = gps.longitude.toBigDecimal()
            it[GpsHistoryTable.speed]        = gps.speed.toBigDecimal()
            it[GpsHistoryTable.heading]      = gps.heading.toBigDecimal()
            it[GpsHistoryTable.dateTime]     = gps.deviceDateTime
            it[GpsHistoryTable.ignition]     = gps.ignition
            it[GpsHistoryTable.satellites]   = gps.satellites
            it[GpsHistoryTable.gpsFixed]     = gps.gpsFixed
            it[GpsHistoryTable.messageType]  = gps.messageType
            it[GpsHistoryTable.gpsQuality]   = gps.gpsQuality.name
            it[GpsHistoryTable.rawLog]       = ""
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
        odometer = null
    )
}

suspend fun updateDeviceLastSeen(imei: String) {
    DatabaseFactory.dbQuery {
        GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) {
            it[lastCommunication] = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Monta resposta padrão GT06:
 * 78 78 [05] [protocolNo] [serialHi] [serialLo] [crcHi] [crcLo] 0D 0A
 */
fun buildResponse(protocolNo: Int, serialNumber: Int): ByteArray {
    val length = 0x05
    val serialHi = (serialNumber shr 8) and 0xFF
    val serialLo = serialNumber and 0xFF

    // CRC calculado sobre: length + protocolNo + serialHi + serialLo
    val crcInput = byteArrayOf(
        length.toByte(),
        protocolNo.toByte(),
        serialHi.toByte(),
        serialLo.toByte()
    )
    val crc = crc16Itu(crcInput)

    return byteArrayOf(
        0x78.toByte(), 0x78.toByte(),
        length.toByte(),
        protocolNo.toByte(),
        serialHi.toByte(), serialLo.toByte(),
        ((crc shr 8) and 0xFF).toByte(), (crc and 0xFF).toByte(),
        0x0D.toByte(), 0x0A.toByte()
    )
}

/**
 * CRC-ITU (CRC-16/CCITT) — algoritmo GT06.
 * Calculado do campo "length" até "serial number" (inclusive).
 */
fun crc16Itu(data: ByteArray): Int {
    var crc = 0xFFFF
    for (b in data) {
        crc = crc xor ((b.toInt() and 0xFF) shl 8)
        repeat(8) {
            crc = if ((crc and 0x8000) != 0) {
                (crc shl 1) xor 0x1021
            } else {
                crc shl 1
            }
            crc = crc and 0xFFFF
        }
    }
    return crc
}

/**
 * Decodifica IMEI em BCD (8 bytes → 15 dígitos).
 * O primeiro nibble é descartado (é sempre 0).
 * Ex: 0x01 0x23 0x45 0x67 0x89 0x01 0x23 0x45 → "123456789012345"
 */
fun decodeBcdImei(bytes: ByteArray): String {
    return bytes.joinToString("") { b ->
        val hi = (b.toInt() shr 4) and 0x0F
        val lo = b.toInt() and 0x0F
        "$hi$lo"
    }.trimStart('0').padStart(15, '0').take(15)
}

fun readUInt32(buf: ByteArray, offset: Int): Long {
    return ((buf[offset].toLong() and 0xFF) shl 24) or
            ((buf[offset + 1].toLong() and 0xFF) shl 16) or
            ((buf[offset + 2].toLong() and 0xFF) shl 8) or
            (buf[offset + 3].toLong() and 0xFF)
}

fun readUInt16(buf: ByteArray, offset: Int): Int {
    return ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)
}

fun readUInt24(buf: ByteArray, offset: Int): Int {
    return ((buf[offset].toInt() and 0xFF) shl 16) or
            ((buf[offset + 1].toInt() and 0xFF) shl 8) or
            (buf[offset + 2].toInt() and 0xFF)
}

fun calculateGpsQuality(satellites: Int?, gpsFixed: Boolean): GpsQuality {
    return when {
        !gpsFixed           -> GpsQuality.NO_SIGNAL
        satellites == null  -> GpsQuality.POOR
        satellites >= 8     -> GpsQuality.EXCELLENT
        satellites >= 5     -> GpsQuality.GOOD
        satellites >= 3     -> GpsQuality.FAIR
        else                -> GpsQuality.POOR
    }
}

fun generateDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d:%02d".format(now.hour, now.minute, now.second)
}

fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)