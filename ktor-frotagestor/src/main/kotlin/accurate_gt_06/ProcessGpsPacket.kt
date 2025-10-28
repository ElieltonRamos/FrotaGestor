package com.frotagestor.accurate_gt_06

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val dateTime: LocalDateTime,
    val ignition: Boolean
)

/**
 * Extrai o IMEI do pacote de login GT06
 */
fun parseImeiFromLogin(data: ByteArray): String {
    return data.sliceArray(4..11).joinToString("") { "%02X".format(it) }
}

/**
 * Parser do pacote GPS GT06
 * Retorna null se os dados forem inválidos
 */
fun parseGpsPacket(data: ByteArray): GpsData? {
    println("Iniciando parse do pacote GPS (${data.size} bytes)")
    println("HEX: ${data.joinToString(" ") { "%02X".format(it) }}")

    // --- Validação de estrutura ---
    if (data.size !in 36..38 ||
        data[0] != 0x78.toByte() || data[1] != 0x78.toByte() ||
        data[data.size - 2] != 0x0D.toByte() || data[data.size - 1] != 0x0A.toByte()
    ) {
        println("Formato inválido: start/stop/tamanho")
        return null
    }

    val payloadLength = data[2].toInt() and 0xFF
    if (payloadLength !in 30..35) return null

    // --- Data/Hora ---
    val dateTime = LocalDateTime(
        2000 + (data[4].toInt() and 0xFF),
        data[5].toInt() and 0xFF,
        data[6].toInt() and 0xFF,
        data[7].toInt() and 0xFF,
        data[8].toInt() and 0xFF,
        data[9].toInt() and 0xFF
    )

    // --- Satélites ---
    val satellites = data[10].toInt() and 0xFF

    // --- Coordenadas ---
    val latRaw = (data[11].toInt() and 0xFF shl 24) or
            (data[12].toInt() and 0xFF shl 16) or
            (data[13].toInt() and 0xFF shl 8) or
            (data[14].toInt() and 0xFF)

    val lonRaw = (data[15].toInt() and 0xFF shl 24) or
            (data[16].toInt() and 0xFF shl 16) or
            (data[17].toInt() and 0xFF shl 8) or
            (data[18].toInt() and 0xFF)

    // Fórmula oficial GT06: graus = valor / 30000 / 60
    var latitude = latRaw / 30000.0 / 60.0
    var longitude = lonRaw / 30000.0 / 60.0

    // --- Velocidade ---
    val speed = data[19].toInt() and 0xFF

    // --- Course + Status ---
    val word = (data[20].toInt() and 0xFF shl 8) or (data[21].toInt() and 0xFF)
    val course = (word and 0x03FF).toDouble()
    val hasFix = (word and 0x0400) != 0   // Bit 10
    val isWest = (word and 0x0800) != 0   // Bit 11
    val isSouth = (word and 0x1000) != 0  // Bit 12
    val ignition = (word and 0x2000) != 0 // Bit 13 (nem todos modelos usam)

    if (isSouth) latitude = -latitude
    if (isWest) longitude = -longitude

    println("lat=$latitude, lon=$longitude, speed=$speed, course=$course, fix=$hasFix, ignition=$ignition")

    // --- Validação ---
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        println("Coordenadas fora do range")
        return null
    }

    // 🚫 Ignorando verificação de hasFix (aceita mesmo sem fix)
    return GpsData(latitude, longitude, speed.toDouble(), course, dateTime, ignition)
}

/**
 * Consulta ID do veículo pelo IMEI
 */
suspend fun findVehicleIdByImei(imei: String): Int? {
    return DatabaseFactory.dbQuery {
        GpsDevicesTable
            .selectAll()
            .where { GpsDevicesTable.imei eq imei }
            .map { it[GpsDevicesTable.vehicleId] }
            .singleOrNull()
    }
}

/**
 * Salva ou atualiza GPS no banco
 */
suspend fun saveOrUpdateGps(imei: String, gps: GpsData) {
    // 🚫 Evita transação aninhada
    val vehicleId = findVehicleIdByImei(imei)
    if (vehicleId == null) {
        println("⚠️ IMEI $imei não vinculado a nenhum veículo — ignorando pacote")
        return
    }

    DatabaseFactory.dbQuery {
        val device = GpsDevicesTable
            .selectAll()
            .where { GpsDevicesTable.imei eq imei }
            .singleOrNull()

        println("🔹 Salvando GPS: vehicleId=$vehicleId, lat=${gps.latitude}, lon=${gps.longitude}, speed=${gps.speed}, heading=${gps.heading}")

        if (device != null) {
            GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) { row ->
                row[GpsDevicesTable.latitude] = gps.latitude.toBigDecimal()
                row[GpsDevicesTable.longitude] = gps.longitude.toBigDecimal()
                row[GpsDevicesTable.speed] = gps.speed.toBigDecimal()
                row[GpsDevicesTable.heading] = (gps.heading % 360.0).toBigDecimal()
                row[GpsDevicesTable.dateTime] = gps.dateTime
                row[GpsDevicesTable.ignition] = gps.ignition
            }
        } else {
            GpsDevicesTable.insert { row ->
                row[GpsDevicesTable.vehicleId] = vehicleId
                row[GpsDevicesTable.imei] = imei
                row[GpsDevicesTable.latitude] = gps.latitude.toBigDecimal()
                row[GpsDevicesTable.longitude] = gps.longitude.toBigDecimal()
                row[GpsDevicesTable.speed] = gps.speed.toBigDecimal()
                row[GpsDevicesTable.heading] = (gps.heading % 360.0).toBigDecimal()
                row[GpsDevicesTable.dateTime] = gps.dateTime
                row[GpsDevicesTable.ignition] = gps.ignition
            }
        }
    }
}