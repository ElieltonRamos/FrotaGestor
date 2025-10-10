package com.frotagestor.database.models

import com.frotagestor.interfaces.VehicleStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object VehiclesTable : Table("vehicles") {
    val id = integer("id").autoIncrement()
    val plate = varchar("plate", 10).uniqueIndex()
    val model = varchar("model", 100)
    val brand = varchar("brand", 100).nullable()
    val year = integer("year").nullable()
    val status = enumerationByName("status", 20, VehicleStatus::class)
    val iconMapUrl = varchar("icon_map_url", 100)
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}