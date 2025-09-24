package com.frotagestor.database.seeds

import com.frotagestor.database.models.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.selectAll

object UserSeed {
    fun run(jdbcUrl: String, user: String, password: String) {
        Database.connect(jdbcUrl, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

        transaction {
            val exists = UsersTable.selectAll().where { UsersTable.username eq "admin" }.count() > 0
            if (!exists) {
                // Gera hash para a senha padrão do admin
                val hashedPassword = BCrypt.withDefaults().hashToString(12, "Ancap2001.".toCharArray())

                UsersTable.insert {
                    it[username] = "Elielton"
                    it[UsersTable.password] = hashedPassword
                    it[role] = "ADMIN"
                }
                println("✅ Usuário Elielton criado (seed).")
            } else {
                println("ℹ️ Usuário Elielton já existe, seed ignorado.")
            }
        }
    }
}
