package com.frotagestor.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object FuelLogsTable : Table("fuel_logs") {
    val id = integer("id").autoIncrement()
    val vehicleId = integer("vehicle_id").references(VehiclesTable.id)
    val driverId = integer("driver_id").references(DriversTable.id).nullable()
    val date = datetime("date")
    val liters = decimal("liters", 10, 2)
    val cost = decimal("cost", 10, 2)
    val station = varchar("station", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}