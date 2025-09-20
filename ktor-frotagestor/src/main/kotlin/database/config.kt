package com.redenorte.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:mysql://localhost:3306/db_frota_gestor",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "appuser",
            password = "apppass"
        )
    }
}
