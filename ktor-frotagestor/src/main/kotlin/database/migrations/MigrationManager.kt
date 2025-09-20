package com.redenorte.database.migrations

import org.flywaydb.core.Flyway
import java.sql.DriverManager

object MigrationManager {

    private const val hostUrl = "jdbc:mysql://localhost:3306/"
    private const val dbName = "db_frota_gestor"
    private const val user = "appuser"
    private const val password = "apppass"

    // Cria o banco de dados se não existir
    fun createDatabaseIfNotExists() {
        DriverManager.getConnection(hostUrl, user, password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS $dbName")
            }
        }
        println("✅ Banco de dados '$dbName' verificado/criado com sucesso.")
    }

    // Executa migrations Flyway
    fun runMigrations() {
        val flyway = Flyway.configure()
            .dataSource("$hostUrl$dbName", user, password)
            .load()

        flyway.migrate()
        println("✅ Migrations aplicadas com sucesso.")
    }

    fun getJdbcUrl(): String = "$hostUrl$dbName"
    fun getUser(): String = user
    fun getPassword(): String = password
}
