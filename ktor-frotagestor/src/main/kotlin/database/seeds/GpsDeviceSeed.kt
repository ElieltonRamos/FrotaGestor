package com.frotagestor.database.seeds

import com.frotagestor.database.models.GpsDevicesTable
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import kotlin.random.Random

object GpsDeviceSeed {
    fun run(jdbcUrl: String, user: String, password: String) {
        Database.connect(jdbcUrl, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

        // Lista de GPS fictícios (vehicleId, imei, lat, long)
        val gpsDevices = listOf(
            Triple(1, "865432109876543", Pair(-23.550520, -46.633308)), // SP
            Triple(2, "865432109876544", Pair(-22.906847, -43.172896)), // RJ
            Triple(3, "865432109876545", Pair(-19.916681, -43.934493)), // BH
            Triple(4, "865432109876546", Pair(-25.428954, -49.267137))  // Curitiba
        )

        transaction {
            gpsDevices.forEach { (vehicleId, imei, coords) ->
                val (lat, long) = coords

                val exists = GpsDevicesTable.selectAll()
                    .where { GpsDevicesTable.vehicleId eq vehicleId }
                    .count() > 0

                if (!exists) {
                    GpsDevicesTable.insert {
                        it[GpsDevicesTable.vehicleId] = vehicleId
                        it[GpsDevicesTable.imei] = imei
                        it[GpsDevicesTable.latitude] = lat.toBigDecimal()
                        it[GpsDevicesTable.longitude] = long.toBigDecimal()
                        it[GpsDevicesTable.dateTime] = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        it[GpsDevicesTable.speed] = BigDecimal(Random.nextDouble(0.0, 120.0))
                        it[GpsDevicesTable.heading] = BigDecimal(Random.nextDouble(0.0, 360.0))
                        it[GpsDevicesTable.iconMapUrl] = "https://example.com/icon.png"
                        it[GpsDevicesTable.title] = "Veículo $vehicleId"
                        it[GpsDevicesTable.ignition] = Random.nextBoolean()
                    }
                } else {
                    println("ℹ️ GPS do veículo $vehicleId já existe, seed ignorado.")
                }
            }
            println("✅ Seed de GPS Devices concluído.")
        }
    }
}
