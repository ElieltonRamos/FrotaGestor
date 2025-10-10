package com.frotagestor.database.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object GpsDevicesTable : Table("gps_devices") {
    val id = integer("id").autoIncrement()
    val vehicleId = integer("vehicle_id").references(VehiclesTable.id)
    val imei = varchar("imei", 50).uniqueIndex()
    val latitude = decimal("latitude", 9, 6)
    val longitude = decimal("longitude", 9, 6)
    val dateTime = datetime("date_time")
    val speed = decimal("speed", 5, 2).default(0.toBigDecimal())
    val heading = decimal("heading", 5, 2).default(0.toBigDecimal())
    val iconMapUrl = varchar("icon_map_url", 255).nullable()
    val title = varchar("title", 255).nullable()
    val ignition = bool("ignition").default(false)

    override val primaryKey = PrimaryKey(id)
}
