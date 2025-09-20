package com.frotagestor.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:mysql://localhost:3306/db_frota_gestor"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = System.getenv("DB_USER") ?: "appuser"
            password = System.getenv("DB_PASS") ?: "apppass"

            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout = 5000

            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        try {
            newSuspendedTransaction(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            // logar / mapear erro
            throw e
        }

}
