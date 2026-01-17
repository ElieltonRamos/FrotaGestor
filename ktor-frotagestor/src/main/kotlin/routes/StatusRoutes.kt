package com.frotagestor.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Serializable
data class StatusResponse(
    val version: String,
    val uptime: String,
    val cpu: String,
    val cpuModel: String,
    val memoryUsed: String,
    val memoryTotal: String,
    val dbStatus: String,
    val dbDetails: String,
    val backupStatus: String,
    val backupLast: String?,
    val backupCount: Int
)

fun Route.statusRoutes(startTime: Instant) {
    get("/status") {
        val status = getStatusInfo(startTime)
        call.respond(status)
    }
}

private suspend fun getStatusInfo(startTime: Instant): StatusResponse = withContext(Dispatchers.IO) {
    val osBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val runtime = Runtime.getRuntime()

    // Memória física do sistema
    val totalPhysicalMemory = osBean.totalPhysicalMemorySize
    val freePhysicalMemory = osBean.freePhysicalMemorySize
    val usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory
    val physicalMemoryPercent = ((usedPhysicalMemory.toDouble() / totalPhysicalMemory) * 100).roundToInt()

    StatusResponse(
        version = getPackageVersion(),
        uptime = formatUptime(startTime),
        cpu = "${runtime.availableProcessors()} cores",
        cpuModel = getCpuModel(),
        memoryUsed = "$physicalMemoryPercent% (${formatBytes(usedPhysicalMemory)})",
        memoryTotal = formatBytes(totalPhysicalMemory),
        dbStatus = getMySqlStatus(),
        dbDetails = "MySQL (Espinosa)",
        backupStatus = getBackupStatus(),
        backupLast = getLastBackup(),
        backupCount = getBackupCount()
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
    } else {
        "1.0.0"
    }
} catch (e: Exception) {
    "dev"
}

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
                        ?.substringAfter(":")
                        ?.trim()
                        ?: "CPU Desconhecida"
                }
            } else "CPU Desconhecida"
        }
        osName.contains("win") -> {
            val process = ProcessBuilder("wmic", "cpu", "get", "name")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                reader.readLines()
                    .drop(1)
                    .firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?: "CPU Desconhecida"
            }
        }
        osName.contains("mac") -> {
            val process = ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().readText().trim()
        }
        else -> "CPU Desconhecida"
    }
} catch (e: Exception) {
    "CPU Desconhecida"
}

private fun getMySqlStatus(): String {
    // TODO: Implementar verificação real de conexão com MySQL
    return "🟢 Online"
}

private fun getBackupStatus(): String {
    val backupDir = File("./backups")
    return when {
        !backupDir.exists() -> "📁 Não existe"
        backupDir.listFiles { _, name -> name.endsWith(".sql") }?.isEmpty() == true -> "⚪ Vazia"
        else -> "🟢 OK"
    }
}

private fun getLastBackup(): String? {
    val backupDir = File("./backups")
    if (!backupDir.exists()) return null

    val backups = backupDir.listFiles { _, name -> name.endsWith(".sql") }
        ?.sortedByDescending { it.lastModified() }
        ?: return null

    return backups.firstOrNull()?.let {
        java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(it.lastModified()))
    }
}

private fun getBackupCount(): Int {
    val backupDir = File("./backups")
    if (!backupDir.exists()) return 0
    return backupDir.listFiles { _, name -> name.endsWith(".sql") }?.size ?: 0
}