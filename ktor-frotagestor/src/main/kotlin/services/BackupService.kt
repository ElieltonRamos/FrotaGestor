package com.frotagestor.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.frotagestor.interfaces.BackupInfo
import com.frotagestor.interfaces.BackupResult

class BackupService(
    private val dbHost: String = "localhost",
    private val dbPort: Int = 3306,
    private val dbName: String = "db_frota_gestor",
    private val dbUser: String = "root",
    private val dbPassword: String = "password",
    private val backupDir: String = "./backups",
    private val maxBackups: Int = 2
) {
    private var backupJob: Job? = null
    private val dataSource: HikariDataSource

    init {
        File(backupDir).mkdirs()

        // Configurar pool de conexões
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$dbHost:$dbPort/$dbName?useSSL=false&allowPublicKeyRetrieval=true"
            username = dbUser
            password = dbPassword
            maximumPoolSize = 5
            minimumIdle = 1
            connectionTimeout = 30000
        }
        dataSource = HikariDataSource(config)
    }

    fun start() {
        backupJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val now = Calendar.getInstance()
                val nextRun = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 2)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)

                    if (before(now)) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val delayMillis = nextRun.timeInMillis - now.timeInMillis
                println("⏰ Próximo backup agendado para: ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(nextRun.time)}")

                delay(delayMillis)

                executeBackup()
                cleanOldBackups()
            }
        }

        println("✅ Serviço de backup iniciado")
    }

    fun stop() {
        backupJob?.cancel()
        dataSource.close()
        println("🛑 Serviço de backup parado")
    }

    suspend fun executeBackup(): BackupResult = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        var writer: FileWriter? = null

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val filename = "backup_${dbName}_$timestamp.sql"
            val backupFile = File(backupDir, filename)

            println("🔄 Iniciando backup via JDBC: $filename")

            connection = dataSource.connection
            writer = FileWriter(backupFile)

            // Cabeçalho do dump
            writer.write("-- MySQL Dump via JDBC\n")
            writer.write("-- Database: $dbName\n")
            writer.write("-- Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\n")
            writer.write("-- ------------------------------------------------------\n\n")
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n")
            writer.write("SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';\n\n")

            // Listar todas as tabelas
            val tables = mutableListOf<String>()
            val tablesRs = connection.metaData.getTables(dbName, null, "%", arrayOf("TABLE"))
            while (tablesRs.next()) {
                tables.add(tablesRs.getString("TABLE_NAME"))
            }
            tablesRs.close()

            println("📋 Encontradas ${tables.size} tabelas para backup")

            // Fazer dump de cada tabela
            for (tableName in tables) {
                writer.write("\n-- Table structure for table `$tableName`\n")
                writer.write("DROP TABLE IF EXISTS `$tableName`;\n")

                // CREATE TABLE
                val createTableStmt = connection.createStatement()
                val createTableRs = createTableStmt.executeQuery("SHOW CREATE TABLE `$tableName`")
                if (createTableRs.next()) {
                    writer.write(createTableRs.getString(2))
                    writer.write(";\n\n")
                }
                createTableRs.close()
                createTableStmt.close()

                // Dados da tabela
                val dataStmt = connection.createStatement()
                val dataRs = dataStmt.executeQuery("SELECT * FROM `$tableName`")
                val metaData = dataRs.metaData
                val columnCount = metaData.columnCount

                var rowCount = 0
                writer.write("-- Dumping data for table `$tableName`\n")

                val insertBuilder = StringBuilder()
                while (dataRs.next()) {
                    if (insertBuilder.isEmpty()) {
                        insertBuilder.append("INSERT INTO `$tableName` VALUES ")
                    } else {
                        insertBuilder.append(",")
                    }

                    insertBuilder.append("(")
                    for (i in 1..columnCount) {
                        if (i > 1) insertBuilder.append(",")

                        val value = dataRs.getString(i)
                        if (value == null) {
                            insertBuilder.append("NULL")
                        } else {
                            val escaped = value
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                            insertBuilder.append("'$escaped'")
                        }
                    }
                    insertBuilder.append(")")
                    rowCount++

                    // Escrever em lotes de 100 linhas
                    if (rowCount % 100 == 0) {
                        insertBuilder.append(";\n")
                        writer.write(insertBuilder.toString())
                        insertBuilder.clear()
                    }
                }

                if (insertBuilder.isNotEmpty()) {
                    insertBuilder.append(";\n")
                    writer.write(insertBuilder.toString())
                }

                writer.write("\n")
                dataRs.close()
                dataStmt.close()

                println("  ✓ $tableName ($rowCount registros)")
            }

            writer.write("\nSET FOREIGN_KEY_CHECKS=1;\n")
            writer.flush()

            val size = backupFile.length()
            println("✅ Backup criado com sucesso: $filename (${formatBytes(size)})")
            BackupResult.Success(filename, size)

        } catch (e: Exception) {
            val errorMsg = "Erro ao criar backup: ${e.message}"
            println("❌ $errorMsg")
            e.printStackTrace()
            BackupResult.Error(errorMsg)
        } finally {
            writer?.close()
            connection?.close()
        }
    }

    private fun cleanOldBackups() {
        val dir = File(backupDir)
        val backups = dir.listFiles { _, name -> name.endsWith(".sql") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (backups.size > maxBackups) {
            backups.drop(maxBackups).forEach { file ->
                if (file.delete()) {
                    println("🗑️ Backup antigo removido: ${file.name}")
                }
            }
        }
    }

    fun getBackupInfo(): BackupInfo {
        val dir = File(backupDir)
        if (!dir.exists()) {
            return BackupInfo(
                status = "📁 Pasta não existe",
                lastBackup = null,
                count = 0
            )
        }

        val backups = dir.listFiles { _, name -> name.endsWith(".sql") }
            ?.sortedByDescending { it.lastModified() }

        return if (backups.isNullOrEmpty()) {
            BackupInfo(
                status = "⚪ Sem backups",
                lastBackup = null,
                count = 0
            )
        } else {
            val lastBackup = backups.first()
            BackupInfo(
                status = "🟢 OK",
                lastBackup = SimpleDateFormat("dd/MM HH:mm").format(Date(lastBackup.lastModified())),
                count = backups.size
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024 * 1024 -> "${(bytes / 1024.0 / 1024 / 1024).roundToInt()} GB"
            bytes >= 1024L * 1024 -> "${(bytes / 1024.0 / 1024).roundToInt()} MB"
            bytes >= 1024L -> "${(bytes / 1024.0).roundToInt()} KB"
            else -> "$bytes B"
        }
    }
}