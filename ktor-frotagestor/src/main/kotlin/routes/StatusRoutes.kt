package com.frotagestor.routes

import com.frotagestor.interfaces.BackupExecuteResponse
import com.frotagestor.interfaces.BackupResult
import com.frotagestor.interfaces.StatusResponse
import com.frotagestor.services.BackupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.io.File
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.roundToInt

private var autoBackupJob: Job? = null

fun Route.statusRoutes(
    startTime: Instant,
    application: Application
) {
    val backupService = BackupService()

    startAutoBackup(backupService, application)

    get("/status") {
        val status = getStatusInfo(startTime, backupService)
        call.respond(status)
    }

    // Endpoint para executar backup manual
    get("/backup") {
        val result = backupService.executeBackup()

        when (result) {
            is BackupResult.Success -> {
                call.respond(
                    HttpStatusCode.OK,
                    BackupExecuteResponse(
                        success = true,
                        message = "Backup criado com sucesso",
                        filename = result.filename,
                        size = formatBytes(result.size)
                    )
                )
            }
            is BackupResult.Error -> {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    BackupExecuteResponse(
                        success = false,
                        message = result.message
                    )
                )
            }
        }
    }
}

private fun startAutoBackup(backupService: BackupService, application: Application) {
    autoBackupJob = CoroutineScope(Dispatchers.Default).launch {
        println("✅ Backup automático iniciado - execução diária às 00:00")

        while (isActive) {
            val now = Calendar.getInstance()
            val nextRun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Se já passou da meia-noite hoje, agendar para amanhã
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val delayMillis = nextRun.timeInMillis - now.timeInMillis
            val nextRunFormatted = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(nextRun.time)
            println("⏰ Próximo backup automático agendado para: $nextRunFormatted")

            delay(delayMillis)

            // Executar backup
            println("🔄 Executando backup automático agendado...")
            val result = backupService.executeBackup()

            when (result) {
                is BackupResult.Success -> {
                    println("✅ Backup automático concluído: ${result.filename}")
                }
                is BackupResult.Error -> {
                    println("❌ Erro no backup automático: ${result.message}")
                }
            }
        }
    }

    // Parar backup automático quando a aplicação for desligada
    application.environment.monitor.subscribe(ApplicationStopped) {
        autoBackupJob?.cancel()
        println("🛑 Backup automático parado")
    }
}

private suspend fun getStatusInfo(
    startTime: Instant,
    backupService: BackupService
): StatusResponse = withContext(Dispatchers.IO) {
    val osBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val runtime = Runtime.getRuntime()

    val totalPhysicalMemory = osBean.totalPhysicalMemorySize
    val freePhysicalMemory = osBean.freePhysicalMemorySize
    val usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory
    val physicalMemoryPercent = ((usedPhysicalMemory.toDouble() / totalPhysicalMemory) * 100).roundToInt()

    val backupInfo = backupService.getBackupInfo()

    StatusResponse(
        version = getPackageVersion(),
        uptime = formatUptime(startTime),
        cpu = "${runtime.availableProcessors()} cores",
        cpuModel = getCpuModel(),
        memoryUsed = "$physicalMemoryPercent% (${formatBytes(usedPhysicalMemory)})",
        memoryTotal = formatBytes(totalPhysicalMemory),
        dbStatus = getMySqlStatus(),
        dbDetails = "MySQL (Espinosa)",
        backupStatus = backupInfo.status,
        backupLast = backupInfo.lastBackup,
        backupCount = backupInfo.count
    )
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> "${(bytes / 1024.0 / 1024 / 1024).roundToInt()} GB"
        bytes >= 1024L * 1024 -> "${(bytes / 1024.0 / 1024).roundToInt()} MB"
        else -> "${(bytes / 1024.0).roundToInt()} KB"
    }
}

private fun getPackageVersion(): String = try {
    val packageJson = File("package.json")
    if (packageJson.exists()) {
        val content = packageJson.readText()
        Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: "1.0.0"
    } else "1.0.0"
} catch (e: Exception) { "dev" }

private fun formatUptime(start: Instant): String {
    val uptime = Duration.between(start, Instant.now())
    val days = uptime.toDays()
    val hours = uptime.toHours() % 24
    val minutes = uptime.toMinutes() % 60

    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0 || days > 0) append("${hours}h ")
        append("${minutes}m")
    }.trim()
}

private fun getCpuModel(): String = try {
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.contains("linux") -> {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists()) {
                cpuInfo.useLines { lines ->
                    lines.firstOrNull { it.startsWith("model name") }
                        ?.substringAfter(":")?.trim() ?: "CPU Desconhecida"
                }
            } else "CPU Desconhecida"
        }
        osName.contains("win") -> {
            val process = ProcessBuilder("wmic", "cpu", "get", "name")
                .redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { reader ->
                reader.readLines().drop(1).firstOrNull { it.isNotBlank() }?.trim() ?: "CPU Desconhecida"
            }
        }
        else -> "CPU Desconhecida"
    }
} catch (e: Exception) { "CPU Desconhecida" }

private fun getMySqlStatus(): String = "🟢 Online"