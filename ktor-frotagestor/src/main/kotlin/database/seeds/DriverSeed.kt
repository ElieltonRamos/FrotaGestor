package com.frotagestor.database.seeds

import com.frotagestor.database.models.DriversTable
import com.frotagestor.interfaces.DriverStatus
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DriverSeed {
    fun run(jdbcUrl: String, user: String, password: String) {
        Database.connect(jdbcUrl, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

        val drivers = listOf(
            Triple("João da Silva", "123.456.789-00", DriverStatus.ATIVO),
            Triple("Maria Oliveira", "987.654.321-00", DriverStatus.ATIVO),
            Triple("Carlos Pereira", "111.222.333-44", DriverStatus.INATIVO),
            Triple("Ana Souza", "555.666.777-88", DriverStatus.ATIVO),
        )

        transaction {
            drivers.forEachIndexed { index, (name, cpf, status) ->
                val exists = DriversTable.selectAll().where { DriversTable.cpf eq cpf }.count() > 0
                if (!exists) {
                    DriversTable.insert {
                        it[DriversTable.name] = name
                        it[DriversTable.cpf] = cpf
                        it[DriversTable.cnh] = "CNH${1000 + index}"
                        it[DriversTable.cnhCategory] = listOf("A", "B", "C", "D").random()
                        it[DriversTable.cnhExpiration] = LocalDate.parse("2028-12-31")
                        it[DriversTable.phone] = "1199${index}888777"
                        it[DriversTable.email] = "${name.lowercase().replace(" ", ".")}@teste.com"
                        it[DriversTable.status] = status
                    }
                } else {
                    println("ℹ️ Motorista $name já existe, seed ignorado.")
                }
            }
            println("✅ Seed de motoristas concluída.")
        }
    }
}
