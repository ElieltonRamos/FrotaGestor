package com.frotagestor.database.seeds

import com.frotagestor.database.models.ExpensesTable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ExpenseSeed {
    fun run(jdbcUrl: String, user: String, password: String) {
        Database.connect(jdbcUrl, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

        val expenses = listOf(
            Triple("Combustível", 250.75, "Abastecimento em posto BR"),
            Triple("Manutenção", 1200.00, "Troca de óleo e revisão"),
            Triple("Pedágio", 35.50, "Viagem SP -> Campinas"),
            Triple("Multa", 180.00, "Excesso de velocidade")
        )

        transaction {
            expenses.forEachIndexed { index, (type, amount, description) ->
                val exists = ExpensesTable
                    .selectAll()
                    .where { (ExpensesTable.type eq type) and (ExpensesTable.amount eq amount.toBigDecimal()) }
                    .count() > 0

                if (!exists) {
                    ExpensesTable.insert {
                        it[vehicleId] = (1..4).random() // associa a algum veículo existente
                        it[driverId] = (1..4).random()  // associa a algum motorista existente
                        it[tripId] = (1..4).random()    // associa a alguma viagem existente
                        it[date] = LocalDate.parse("2025-09-24").plus(DatePeriod(days = index))
                        it[ExpensesTable.type] = type
                        it[ExpensesTable.description] = description
                        it[ExpensesTable.amount] = amount.toBigDecimal()
                    }
                } else {
                    println("ℹ️ Despesa $type já existe, seed ignorado.")
                }
            }
            println("✅ Seed de despesas concluída.")
        }
    }
}
