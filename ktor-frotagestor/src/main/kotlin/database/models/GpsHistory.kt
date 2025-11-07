package com.frotagestor.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object GpsHistoryTable : Table("gps_history") {
    val id = long("id").autoIncrement()  // BIGINT AUTO_INCREMENT
    val gpsDeviceId = integer("gps_device_id").references(GpsDevicesTable.id)
    val vehicleId = integer("vehicle_id").references(VehiclesTable.id).nullable()
    val dateTime = datetime("date_time")
    val speed = decimal("speed", 5, 2).default(0.toBigDecimal())
    val latitude = decimal("latitude", 9, 6)
    val longitude = decimal("longitude", 9, 6)
    val rawLog = text("raw_log")

    init {
        index(true, gpsDeviceId, dateTime)  // idx_device_time
        index(false, dateTime)              // idx_datetime
        index(false, vehicleId, dateTime)   // idx_vehicle_time
    }

    override val primaryKey = PrimaryKey(id)
}
