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

fun parseImeiFromLogin(data: ByteArray): String {
    // O IMEI está nos bytes 4 a 11 (8 bytes) geralmente
    return data.sliceArray(4..11).joinToString("") { "%02X".format(it) }
}

fun parseGpsPacket(data: ByteArray): GpsData {
    val year = 2000 + (data[4].toInt() and 0xFF)
    val month = data[5].toInt() and 0xFF
    val day = data[6].toInt() and 0xFF
    val hour = data[7].toInt() and 0xFF
    val minute = data[8].toInt() and 0xFF
    val second = data[9].toInt() and 0xFF
    val dateTime = LocalDateTime(year, month, day, hour, minute, second)

    val latRaw = ((data[10].toInt() and 0xFF) shl 24) or
            ((data[11].toInt() and 0xFF) shl 16) or
            ((data[12].toInt() and 0xFF) shl 8) or
            (data[13].toInt() and 0xFF)
    val latitude = latRaw / 30000.0

    val lonRaw = ((data[14].toInt() and 0xFF) shl 24) or
            ((data[15].toInt() and 0xFF) shl 16) or
            ((data[16].toInt() and 0xFF) shl 8) or
            (data[17].toInt() and 0xFF)
    val longitude = lonRaw / 30000.0

    val speed = (data[18].toInt() and 0xFF).toDouble()
    val heading = (data[19].toInt() and 0xFF).toDouble()
    val ignition = (data[20].toInt() and 0x01) != 0

    return GpsData(latitude, longitude, speed, heading, dateTime, ignition)
}

suspend fun findVehicleIdByImei(imei: String): Int? {
    return DatabaseFactory.dbQuery {
        GpsDevicesTable
            .selectAll().where { GpsDevicesTable.imei eq imei }
            .map { it[GpsDevicesTable.vehicleId] }
            .singleOrNull()
    }
}

suspend fun saveOrUpdateGps(imei: String, gps: GpsData) {
    DatabaseFactory.dbQuery {
        val device = GpsDevicesTable.selectAll().where { GpsDevicesTable.imei eq imei }.singleOrNull()

        val vehicleId = findVehicleIdByImei(imei) ?: 0

        if (device != null) {
            GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) { row ->
                row[GpsDevicesTable.latitude] = gps.latitude.toBigDecimal()
                row[GpsDevicesTable.longitude] = gps.longitude.toBigDecimal()
                row[GpsDevicesTable.speed] = gps.speed.toBigDecimal()
                row[GpsDevicesTable.heading] = gps.heading.toBigDecimal()
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
                row[GpsDevicesTable.heading] = gps.heading.toBigDecimal()
                row[GpsDevicesTable.dateTime] = gps.dateTime
                row[GpsDevicesTable.ignition] = gps.ignition
            }
        }
    }
}