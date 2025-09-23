package com.frotagestor.database.models

import com.frotagestor.interfaces.DriverStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object DriversTable : Table("drivers") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val cpf = varchar("cpf", 14).uniqueIndex()
    val cnh = varchar("cnh", 20).uniqueIndex()
    val cnhCategory = varchar("cnh_category", 5).nullable()
    val cnhExpiration = date("cnh_expiration").nullable()
    val phone = varchar("phone", 20).nullable()
    val email = varchar("email", 100).nullable()
    val status = enumerationByName("status", 20, DriverStatus::class)

    override val primaryKey = PrimaryKey(id)
}