package com.redenorte.database.migrations

import com.redenorte.database.seeds.DatabaseSeeder

fun main() {
    MigrationManager.createDatabaseIfNotExists()
    MigrationManager.runMigrations()
    DatabaseSeeder.run()
}
