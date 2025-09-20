package com.redenorte.repositorys

import com.redenorte.database.models.UsersTable
import com.redenorte.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

    fun getAll(): List<User> = transaction {
        UsersTable.selectAll().map {
            User(
                id = it[UsersTable.id],
                username = it[UsersTable.username],
                password = it[UsersTable.password],
                role = it[UsersTable.role]
            )
        }
    }

    fun findByUsername(username: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.username eq username }
            .map {
                User(
                    id = it[UsersTable.id],
                    username = it[UsersTable.username],
                    password = it[UsersTable.password],
                    role = it[UsersTable.role]
                )
            }.singleOrNull()
    }

    fun create(user: User): User = transaction {
        val id = UsersTable.insert {
            it[username] = user.username
            it[password] = user.password
            it[role] = user.role
        }
        user.copy(id = 2)
    }
}
