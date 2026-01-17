package com.frotagestor.services

import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class BackupService(
    private val dbHost: String = "localhost",
    private val dbPort: Int = 3306,
    private val dbName: String = "frotagestor",
    private val dbUser: String = "root",
    private val dbPassword: String = "sua_senha",
    private val backupDir: String = "./backups",
    private val maxBackups: Int = 2
) {
    private var backupJob: Job? = null
    private val mysqlDumpPath: String by lazy { findMysqlDump() }

    init {
        File(backupDir).mkdirs()
    }

    /**
     * Tenta encontrar o mysqldump no sistema
     */
    private fun findMysqlDump(): String {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("linux") || osName.contains("mac") -> {
                // Tentar encontrar mysqldump no PATH do Linux/Mac
                val possiblePaths = listOf(
                    "/usr/bin/mysqldump",
                    "/usr/local/bin/mysqldump",
                    "/opt/homebrew/bin/mysqldump",
                    "mysqldump"
                )

                possiblePaths.firstOrNull { path ->
                    try {
                        ProcessBuilder("which", path).start().waitFor() == 0
                    } catch (e: Exception) {
                        File(path).exists()
                    }
                } ?: run {
                    println("⚠️ mysqldump não encontrado. Tentando usar 'mysqldump' do PATH")
                    "mysqldump"
                }
            }
            osName.contains("win") -> {
                // Caminhos comuns no Windows
                val possiblePaths = listOf(
                    "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                    "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe",
                    "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
                    "C:\\xampp\\mysql\\bin\\mysqldump.exe",
                    "mysqldump.exe"
                )

                possiblePaths.firstOrNull { File(it).exists() } ?: run {
                    println("⚠️ mysqldump.exe não encontrado. Configure o caminho manualmente.")
                    "mysqldump.exe"
                }
            }
            else -> "mysqldump"
        }
    }

    fun start() {
        println("🔍 Caminho do mysqldump: $mysqlDumpPath")

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
        println("🛑 Serviço de backup parado")
    }

    suspend fun executeBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val filename = "backup_${dbName}_$timestamp.sql"
            val backupFile = File(backupDir, filename)

            println("🔄 Iniciando backup: $filename")
            println("📍 Usando mysqldump: $mysqlDumpPath")

            // Verificar se mysqldump existe
            if (!File(mysqlDumpPath).exists() && mysqlDumpPath != "mysqldump" && mysqlDumpPath != "mysqldump.exe") {
                val error = "mysqldump não encontrado em: $mysqlDumpPath"
                println("❌ $error")
                return@withContext BackupResult.Error(error)
            }

            val command = buildList {
                add(mysqlDumpPath)
                add("-h"); add(dbHost)
                add("-P"); add(dbPort.toString())
                add("-u"); add(dbUser)
                add("-p$dbPassword")
                add("--single-transaction")
                add("--routines")
                add("--triggers")
                add(dbName)
            }

            val process = ProcessBuilder(command)
                .redirectOutput(backupFile)
                .redirectErrorStream(false)
                .start()

            val exitCode = process.waitFor()

            if (exitCode == 0 && backupFile.exists() && backupFile.length() > 0) {
                val size = formatBytes(backupFile.length())
                println("✅ Backup criado com sucesso: $filename ($size)")
                BackupResult.Success(filename, backupFile.length())
            } else {
                val error = process.errorStream.bufferedReader().readText().ifBlank { "Erro desconhecido (exit code: $exitCode)" }
                println("❌ Erro ao criar backup: $error")
                backupFile.delete()
                BackupResult.Error(error)
            }
        } catch (e: Exception) {
            val errorMsg = "Exceção ao criar backup: ${e.message}"
            println("❌ $errorMsg")
            e.printStackTrace()
            BackupResult.Error(errorMsg)
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

// Classes de resultado
sealed class BackupResult {
    data class Success(val filename: String, val size: Long) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

data class BackupInfo(
    val status: String,
    val lastBackup: String?,
    val count: Int
)