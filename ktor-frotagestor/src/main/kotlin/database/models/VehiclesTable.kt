package com.frotagestor.database.models

import com.frotagestor.interfaces.VehicleStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object VehiclesTable : Table("vehicles") {
    val id: Column<Int> = integer("id").autoIncrement()
    val plate = varchar("plate", 10).uniqueIndex()
    val model = varchar("model", 100)
    val brand = varchar("brand", 100).nullable()
    val year = integer("year").nullable()
    val defaultDriverId = optReference(
        "default_driver_id",
        DriversTable.id,
        onDelete = ReferenceOption.SET_NULL
    )
    val status = enumerationByName("status", 20, VehicleStatus::class)
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}