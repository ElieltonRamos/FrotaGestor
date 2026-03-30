package com.frotagestor.protocols_devices_gps.gt06

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import com.frotagestor.services.DailyTripService
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

private val dailyTripService = DailyTripService()

// ─── Protocol Numbers ────────────────────────────────────────────────────────
object GT06Protocol {
    const val LOGIN      = 0x01
    const val GPS        = 0x10
    const val LBS        = 0x11  // LBS only — não implementado
    const val GPS_LBS    = 0x12
    const val HEARTBEAT  = 0x13
    const val ALARM      = 0x16  // GPS + LBS + Status merged
    const val CMD_REPLY  = 0x15  // device → server
    const val CMD_SERVER = 0x80  // server → device
    const val GT06_EXT   = 0x60  // Kingwo extended — LT32 PRO (sem ACK, IMEI nos primeiros 8 bytes)
}

// ─── Alarm types ─────────────────────────────────────────────────────────────
// 0x16 e 0x60 compartilham alarmType=4 para ACC ON/OFF — sem distinção observada
object GT06AlarmType {
    const val ACC = 4   // observado: ACC ON e ACC OFF
    fun describe(code: Int): String = when (code) {
        ACC -> "ACC"
        else -> "UNKNOWN($code)"
    }
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
        append(" | gsm=").append(gsmSignal)
        append(" | alarm=").append(alarmType?.let { GT06AlarmType.describe(it) })
        append(" | sn=").append(serialNumber)
    }
}

// ─── Entry point ─────────────────────────────────────────────────────────────
suspend fun processPacketGT06(packet: ByteArray, currentImei: String?): GT06ProcessResult {
    if (packet.size < 5) return GT06ProcessResult()

    val protocolNo   = packet[3].toInt() and 0xFF
    val contentEnd   = packet.size - 5
    val content      = if (contentEnd > 4) packet.copyOfRange(4, contentEnd) else ByteArray(0)
    val serialNumber = ((packet[packet.size - 5].toInt() and 0xFF) shl 8) or
            (packet[packet.size - 4].toInt() and 0xFF)

    return when (protocolNo) {
        GT06Protocol.LOGIN     -> handleLogin(packet, content, serialNumber)
        GT06Protocol.HEARTBEAT -> handleHeartbeat(content, serialNumber, currentImei)
        GT06Protocol.GPS       -> handleGps(content, serialNumber, currentImei, "GPS")
        GT06Protocol.GPS_LBS   -> handleGps(content, serialNumber, currentImei, "GPS_LBS")
        GT06Protocol.ALARM     -> handleAlarm(content, serialNumber, currentImei)
        GT06Protocol.GT06_EXT  -> handleGT06Ext(content, currentImei)
        GT06Protocol.CMD_REPLY -> handleCommandReply(content, currentImei)
        else -> {
            println("[${generateDate()}] ⚠️ Protocol desconhecido: 0x${"%02X".format(protocolNo)}")
            GT06ProcessResult()
        }
    }
}

// ─── LOGIN (0x01) ─────────────────────────────────────────────────────────────
private fun handleLogin(packet: ByteArray, content: ByteArray, serialNumber: Int): GT06ProcessResult {
    if (content.size < 8) {
        println("[${generateDate()}] ❌ Login packet muito curto")
        return GT06ProcessResult()
    }

    val imei = decodeBcdImei(content.copyOfRange(0, 8))
    println("[${generateDate()}] 🔑 LOGIN | IMEI=$imei")

    return GT06ProcessResult(imei = imei, response = buildResponse(GT06Protocol.LOGIN, serialNumber))
}

// ─── HEARTBEAT (0x13) ────────────────────────────────────────────────────────
// Layout: terminalInfo(1) + voltageLevel(1) + gsmSignal(1) + alarm/lang(2)
// Ignição: bit 1 do terminalInfo (0x44=desligada, 0x46=ligada)
private suspend fun handleHeartbeat(content: ByteArray, serialNumber: Int, imei: String?): GT06ProcessResult {
    val terminalInfo = content.getOrNull(0)?.toInt()?.and(0xFF)
    val gsmSignal    = content.getOrNull(2)?.toInt()?.and(0xFF)
    val ignition     = terminalInfo?.let { (it shr 1) and 0x01 == 1 }

    println("[${generateDate()}] ❤️ HEARTBEAT | IMEI=$imei | ign=$ignition | gsm=$gsmSignal")

    if (imei != null) updateDeviceLastSeen(imei)

    return GT06ProcessResult(response = buildResponse(GT06Protocol.HEARTBEAT, serialNumber))
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
    println("[${generateDate()}] 📍 ${gps.toCompactLog()}")

    saveOrUpdateGps(imei, gps)
    return GT06ProcessResult()
}

// ─── ALARM (0x16) ────────────────────────────────────────────────────────────
// Layout real confirmado empiricamente:
// [26] = byte fixo (0x0F) — ignorado
// [27] = terminalInfo real — bit 1 = ignição (mesmo encoding do heartbeat)
// [28] = gsmSignal
// [29] = alarmType
private suspend fun handleAlarm(content: ByteArray, serialNumber: Int, imei: String?): GT06ProcessResult {
    if (imei == null) {
        println("[${generateDate()}] ❌ ALARM recebido sem IMEI registrado")
        return GT06ProcessResult()
    }

    val gps = parseGpsContent(
        content             = content,
        imei                = imei,
        type                = "ALARM",
        serialNumber        = serialNumber,
        ignitionBit         = 1,
        terminalInfoOffset  = 27
    ) ?: return GT06ProcessResult()

    println("[${generateDate()}] 🚨 ALARM | ${gps.toCompactLog()}")
    logAlarmDebug(content)

    saveOrUpdateGps(imei, gps)

    return GT06ProcessResult(response = buildResponse(GT06Protocol.ALARM, serialNumber))
}

// ─── GT06_EXT (0x60) — Kingwo/LT32 PRO ──────────────────────────────────────
// Estrutura: [IMEI: 8 bytes BCD] + mesmo layout do 0x16
// Sem ACK (tcp_ack=false no perfil)
// ignitionBit=4, terminalInfoOffset=27: aguardando validação com logs reais
private suspend fun handleGT06Ext(content: ByteArray, currentImei: String?): GT06ProcessResult {
    if (content.size < 8) {
        println("[${generateDate()}] ❌ GT06_EXT packet muito curto")
        return GT06ProcessResult()
    }

    val imei = decodeBcdImei(content.copyOfRange(0, 8))

    val resolvedImei = if (currentImei == null) {
        println("[${generateDate()}] ℹ️ GT06_EXT — IMEI extraído do pacote: $imei")
        imei
    } else {
        currentImei
    }

    val gpsContent = content.copyOfRange(8, content.size)

    val gps = parseGpsContent(
        content             = gpsContent,
        imei                = resolvedImei,
        type                = "GT06_EXT",
        serialNumber        = 0,
        ignitionBit         = 4,
        terminalInfoOffset  = 27
    ) ?: return GT06ProcessResult(imei = imei)

    println("[${generateDate()}] 📍 GT06_EXT | ${gps.toCompactLog()}")
    logAlarmDebug(gpsContent)

    saveOrUpdateGps(resolvedImei, gps)

    return GT06ProcessResult(imei = imei, response = null)
}

// ─── CMD REPLY (0x15) ────────────────────────────────────────────────────────
private fun handleCommandReply(content: ByteArray, imei: String?): GT06ProcessResult {
    val text = content.toString(Charsets.US_ASCII).trim()
    println("[${generateDate()}] 📨 CMD REPLY | IMEI=$imei | $text")
    return GT06ProcessResult()
}

// ─── Parser de conteúdo GPS ──────────────────────────────────────────────────
/**
 * Layout (0x10 / 0x12 / 0x16 / GT06_EXT):
 * [0-5]   DateTime: year(+2000), month, day, hour, minute, second
 * [6]     GPS info: high nibble = length, low nibble = satellites
 * [7-10]  Latitude  (uint32 / 1_800_000.0)
 * [11-14] Longitude (uint32 / 1_800_000.0)
 * [15]    Speed (km/h)
 * [16]    Course byte1: bit4=gpsFixed, bit3=West, bit2=South, bit1-0=heading high
 * [17]    Course byte2: heading low (0-255)
 * [18-19] MCC
 * [20]    MNC
 * [21-22] LAC
 * [23-25] Cell ID (3 bytes)
 * [26]    0x0F fixo no 0x16 (ignorado) / terminalInfo no 0x10/0x12
 * [27]    terminalInfo no 0x16/GT06_EXT — bit 1 = ignição
 * [28]    gsmSignal
 * [29]    alarmType
 * [30]    language
 *
 * terminalInfoOffset: 26 para 0x10/0x12, 27 para 0x16/GT06_EXT
 */
fun parseGpsContent(
    content: ByteArray,
    imei: String,
    type: String,
    serialNumber: Int,
    ignitionBit: Int = 1,
    terminalInfoOffset: Int = 26
): GpsData? {
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

        val satellites  = content[6].toInt() and 0x0F
        val latRaw      = readUInt32(content, 7)
        val lonRaw      = readUInt32(content, 11)
        var latitude    = latRaw / 1_800_000.0
        var longitude   = lonRaw / 1_800_000.0

        val courseByte1 = content[16].toInt() and 0xFF
        val courseByte2 = content[17].toInt() and 0xFF

        val gpsFixed = (courseByte1 shr 4) and 0x01 == 1
        val isWest   = (courseByte1 shr 3) and 0x01 == 1
        val isSouth  = (courseByte1 shr 2) and 0x01 == 1

        if (isWest)  longitude = -longitude
        if (isSouth) latitude  = -latitude

        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            println("❌ Coordenadas fora dos limites: lat=$latitude lon=$longitude")
            return null
        }

        val speed   = (content[15].toInt() and 0xFF).toDouble()
        val heading = (((courseByte1 and 0x03) shl 8) or courseByte2).toDouble()

        val mcc    = if (content.size > 19) readUInt16(content, 18) else null
        val mnc    = if (content.size > 20) content[20].toInt() and 0xFF else null
        val lac    = if (content.size > 22) readUInt16(content, 21) else null
        val cellId = if (content.size > 25) readUInt24(content, 23) else null

        val terminalInfo = if (content.size > terminalInfoOffset) content[terminalInfoOffset].toInt() and 0xFF else null
        val gsmSignal    = if (content.size > terminalInfoOffset + 1) content[terminalInfoOffset + 1].toInt() and 0xFF else null
        val alarmByte    = if (content.size > terminalInfoOffset + 2) content[terminalInfoOffset + 2].toInt() and 0xFF else null

        val ignition = terminalInfo?.let { (it shr ignitionBit) and 0x01 == 1 } ?: false

        val deviceDateTime = try {
            val utc = LocalDateTime(year, month, day, hour, minute, second)
            utc.toInstant(TimeZone.UTC).toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        GpsData(
            latitude       = latitude,
            longitude      = longitude,
            speed          = speed,
            heading        = heading % 360,
            ignition       = ignition,
            deviceDateTime = deviceDateTime,
            serverDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            messageType    = type,
            deviceId       = imei,
            satellites     = satellites,
            gpsFixed       = gpsFixed,
            gpsQuality     = calculateGpsQuality(satellites, gpsFixed),
            mcc            = mcc,
            mnc            = mnc,
            lac            = lac,
            cellId         = cellId,
            gsmSignal      = gsmSignal,
            alarmType      = alarmByte,
            serialNumber   = serialNumber
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
        imei      = imei,
        vehicleId = vehicleId,
        latitude  = gps.latitude,
        longitude = gps.longitude,
        dateTime  = gps.serverDateTime,
        speed     = gps.speed,
        ignition  = gps.ignition,
        odometer  = null
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

suspend fun findVehicleIdByImei(imei: String): Int? {
    return DatabaseFactory.dbQuery {
        GpsDevicesTable
            .selectAll()
            .where { GpsDevicesTable.imei eq imei }
            .singleOrNull()
            ?.let { it[GpsDevicesTable.vehicleId] }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fun buildResponse(protocolNo: Int, serialNumber: Int): ByteArray {
    val length   = 0x05
    val serialHi = (serialNumber shr 8) and 0xFF
    val serialLo = serialNumber and 0xFF

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

fun crc16Itu(data: ByteArray): Int {
    var crc = 0xFFFF
    for (b in data) {
        crc = crc xor ((b.toInt() and 0xFF) shl 8)
        repeat(8) {
            crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
            crc = crc and 0xFFFF
        }
    }
    return crc
}

fun decodeBcdImei(bytes: ByteArray): String {
    val full = bytes.joinToString("") { b ->
        val hi = (b.toInt() shr 4) and 0x0F
        val lo = b.toInt() and 0x0F
        "$hi$lo"
    }
    return full.substring(1, 16)
}

fun readUInt32(buf: ByteArray, offset: Int): Long =
    ((buf[offset].toLong() and 0xFF) shl 24) or
            ((buf[offset + 1].toLong() and 0xFF) shl 16) or
            ((buf[offset + 2].toLong() and 0xFF) shl 8) or
            (buf[offset + 3].toLong() and 0xFF)

fun readUInt16(buf: ByteArray, offset: Int): Int =
    ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)

fun readUInt24(buf: ByteArray, offset: Int): Int =
    ((buf[offset].toInt() and 0xFF) shl 16) or
            ((buf[offset + 1].toInt() and 0xFF) shl 8) or
            (buf[offset + 2].toInt() and 0xFF)

fun calculateGpsQuality(satellites: Int?, gpsFixed: Boolean): GpsQuality = when {
    !gpsFixed          -> GpsQuality.NO_SIGNAL
    satellites == null -> GpsQuality.POOR
    satellites >= 8    -> GpsQuality.EXCELLENT
    satellites >= 5    -> GpsQuality.GOOD
    satellites >= 3    -> GpsQuality.FAIR
    else               -> GpsQuality.POOR
}

// Manter ativo até todos os alarmType relevantes serem identificados
private fun logAlarmDebug(content: ByteArray) {
    val terminalInfo = content.getOrNull(27)?.toInt()?.and(0xFF)
    val alarmByte    = content.getOrNull(29)?.toInt()?.and(0xFF)
    if (terminalInfo != null || alarmByte != null) {
        println(
            "[ALARM_DEBUG] termInfo=${terminalInfo?.toString(2)?.padStart(8, '0')} " +
                    "| alarmRaw=$alarmByte | alarmDesc=${alarmByte?.let { GT06AlarmType.describe(it) }}"
        )
    }
}

fun generateDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d:%02d".format(now.hour, now.minute, now.second)
}

fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)