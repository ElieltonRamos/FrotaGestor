package com.frotagestor.database.models

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object SubfleetsTable : Table("subfleets") {
    val id: Column<Int> = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}
