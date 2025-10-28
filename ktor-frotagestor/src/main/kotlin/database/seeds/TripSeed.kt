package com.frotagestor.database.seeds

import com.frotagestor.database.models.TripsTable
import com.frotagestor.interfaces.TripStatus
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object TripSeed {
    fun run(jdbcUrl: String, user: String, password: String) {
        Database.connect(jdbcUrl, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

        val trips = listOf(
            Triple(1 to 1, "São Paulo - SP", "Campinas - SP"),
            Triple(2 to 2, "Rio de Janeiro - RJ", "Niterói - RJ"),
            Triple(3 to 1, "Belo Horizonte - MG", "Contagem - MG"),
            Triple(4 to 3, "Curitiba - PR", "Joinville - SC"),
        )

        transaction {
            trips.forEachIndexed { index, (ids, start, end) ->
                val (vehicleId, driverId) = ids
                val exists = TripsTable.selectAll()
                    .where { (TripsTable.vehicleId eq vehicleId) and (TripsTable.driverId eq driverId) }
                    .count() > 0

                if (!exists) {
                    TripsTable.insert {
                        it[TripsTable.vehicleId] = vehicleId
                        it[TripsTable.driverId] = driverId
                        it[startLocation] = start
                        it[endLocation] = end
                        it[startTime] = LocalDateTime.parse("2025-09-24T08:00:00")
                        it[endTime] = LocalDateTime.parse("2025-09-24T10:00:00")
                        it[distanceKm] = (50..300).random().toBigDecimal()
                        it[status] = listOf(TripStatus.PLANEJADA, TripStatus.EM_ANDAMENTO, TripStatus.CONCLUIDA).random()
                    }
                } else {
                    println("ℹ️ Viagem de $start até $end já existe, seed ignorado.")
                }
            }
            println("✅ Seed de viagens concluída.")
        }
    }
}
