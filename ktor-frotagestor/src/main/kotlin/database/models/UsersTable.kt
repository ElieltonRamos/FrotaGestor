package com.redenorte.database.models

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 255)
    val role = varchar("role", 20)

    override val primaryKey = PrimaryKey(id)
}
