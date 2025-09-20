package com.frotagestor.database.migrations

import com.frotagestor.database.seeds.DatabaseSeeder

fun main() {
    MigrationManager.createDatabaseIfNotExists()
    MigrationManager.runMigrations()
    DatabaseSeeder.run()
}
