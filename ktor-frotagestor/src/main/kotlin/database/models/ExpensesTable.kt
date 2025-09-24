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
    // 🔹 Campos específicos para abastecimento (opcionais)
    val liters = decimal("liters", 10, 2).nullable()           // Quantidade de litros
    val pricePerLiter = decimal("price_per_liter", 10, 2).nullable() // Preço por litro
    val odometer = integer("odometer").nullable()              // Km do veículo no momento


    override val primaryKey = PrimaryKey(id)
}