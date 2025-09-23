package com.frotagestor.database.models

import com.frotagestor.interfaces.TripStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object TripsTable : Table("trips") {
    val id = integer("id").autoIncrement()
    val vehicleId = integer("vehicle_id").references(VehiclesTable.id)
    val driverId = integer("driver_id").references(DriversTable.id)
    val startLocation = varchar("start_location", 255).nullable()
    val endLocation = varchar("end_location", 255).nullable()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time").nullable()
    val distanceKm = decimal("distance_km", 10, 2).nullable()
    val status = enumerationByName("status", 20, TripStatus::class)

    override val primaryKey = PrimaryKey(id)
}