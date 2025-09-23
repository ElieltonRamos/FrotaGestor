package com.frotagestor.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object ExpensesTable : Table("expenses") {
    val id = integer("id").autoIncrement()
    val vehicleId = integer("vehicle_id").references(VehiclesTable.id).nullable()
    val driverId = integer("driver_id").references(DriversTable.id).nullable()
    val tripId = integer("trip_id").references(TripsTable.id).nullable()
    val date = date("date")
    val type = varchar("type", 50)
    val description = text("description").nullable()
    val amount = decimal("amount", 10, 2)

    override val primaryKey = PrimaryKey(id)
}