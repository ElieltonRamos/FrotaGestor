package com.frotagestor.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object GpsHistoryTable : Table("gps_history") {

    val id = long("id").autoIncrement()

    val gpsDeviceId = integer("gps_device_id")
        .references(GpsDevicesTable.id)

    val vehicleId = integer("vehicle_id")
        .references(VehiclesTable.id)
        .nullable()

    val dateTime = datetime("date_time")

    val latitude = decimal("latitude", 9, 6)
    val longitude = decimal("longitude", 9, 6)

    val speed = decimal("speed", 5, 2).default(0.toBigDecimal())
    val heading = decimal("heading", 6, 2).default(0.toBigDecimal())

    val ignition = bool("ignition").default(false)

    val satellites = integer("satellites").nullable()
    val gpsFixed = bool("gps_fixed").default(false)

    val odometer = long("odometer").nullable()

    val batteryVoltage = decimal("battery_voltage", 5, 2).nullable()

    val messageType = varchar("message_type", 20)
    val eventCode = integer("event_code").nullable()

    val gpsQuality = varchar("gps_quality", 20)

    val rawLog = text("raw_log")

    init {
        index(true, gpsDeviceId, dateTime)   // idx_device_time (UNIQUE)
        index(false, dateTime)               // idx_datetime
        index(false, vehicleId, dateTime)    // idx_vehicle_time
    }

    override val primaryKey = PrimaryKey(id)
}
