package com.frotagestor.database.seeds

import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.VehicleStatus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object VehicleSeed {
    fun run(jdbcUrl: String, user: String, password: String) {
        Database.connect(jdbcUrl, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

        val vehicles = listOf(
            Triple("ABC1D23", "Fiesta", VehicleStatus.ATIVO),
            Triple("XYZ9K87", "Civic", VehicleStatus.MANUTENCAO),
            Triple("MNO4L56", "Onix", VehicleStatus.INATIVO),
            Triple("JKL2P34", "Corolla", VehicleStatus.ATIVO),
        )

        transaction {
            vehicles.forEach { (plate, model, status) ->
                val exists = VehiclesTable.selectAll().where { VehiclesTable.plate eq plate }.count() > 0
                if (!exists) {
                    VehiclesTable.insert {
                        it[VehiclesTable.plate] = plate
                        it[VehiclesTable.model] = model
                        it[VehiclesTable.brand] = listOf("Ford", "Honda", "Chevrolet", "Toyota").random()
                        it[VehiclesTable.year] = (2015..2022).random()
                        it[VehiclesTable.status] = status
                    }
                } else {
                    println("ℹ️ Veículo $plate já existe, seed ignorado.")
                }
            }
            println("✅ Seed de veículos concluída.")
        }
    }
}
