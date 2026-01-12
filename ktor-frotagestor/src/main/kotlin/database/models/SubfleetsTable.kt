package com.frotagestor.database.models

import com.frotagestor.interfaces.SubfleetStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object SubfleetsTable : Table("subfleets") {
    val id: Column<Int> = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = text("description").nullable()

    // Self-reference para hierarquia (subfrota pai)
    val parentId = optReference(
        "parent_id",
        id,
        onDelete = ReferenceOption.SET_NULL
    )

    val color = varchar("color", 7).default("#3B82F6")
    val icon = varchar("icon", 50).default("truck")

    val managerUserId = optReference(
        "manager_user_id",
        UsersTable.id,
        onDelete = ReferenceOption.SET_NULL
    )

    val status = enumerationByName("status", 20, SubfleetStatus::class)
        .default(SubfleetStatus.ACTIVE)

    val createdAt = datetime("created_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }

    override val primaryKey = PrimaryKey(id)
}
