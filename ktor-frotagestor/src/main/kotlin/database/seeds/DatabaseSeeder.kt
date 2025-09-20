package com.frotagestor.database.seeds

import com.frotagestor.database.migrations.MigrationManager

object DatabaseSeeder {
    fun run() {
        val jdbcUrl = MigrationManager.getJdbcUrl()
        val user = MigrationManager.getUser()
        val password = MigrationManager.getPassword()

        println("🚀 Executando seeds...")

        UserSeed.run(jdbcUrl, user, password)

        println("✅ Seeds executados com sucesso.")
    }
}
