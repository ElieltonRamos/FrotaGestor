package com.frotagestor.accurate_suntech_st310

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

suspend fun startTcpServerSuntech() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", 3002)
    println("[${now()}] Servidor TCP rodando na porta 3002 – protocolo Suntech ST310/ST300")

    while (true) {
        val socket = serverSocket.accept()
        println("[${now()}] Nova conexão: ${socket.remoteAddress}")
        GlobalScope.launch { handleDevice(socket) }
    }
}

suspend fun handleDevice(socket: Socket) {
    val input  = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)
    var deviceId: String? = null
    val buffer = StringBuilder()

    try {
        while (!input.isClosedForRead) {
            val bytes = ByteArray(1024)
            val read = input.readAvailable(bytes)
            if (read > 0) {
                val rawBytes = bytes.copyOf(read)
                val ascii = rawBytes.toString(Charsets.US_ASCII)

                buffer.append(ascii)

                // Processa mensagens completas terminadas em \r ou \n
                var processed: Int
                do {
                    processed = 0
                    val fullMsg = buffer.toString()
                    val crIndex = fullMsg.indexOf('\r')
                    val lfIndex = fullMsg.indexOf('\n')

                    val endIndex = when {
                        crIndex >= 0 && lfIndex == crIndex + 1 -> crIndex
                        crIndex >= 0 -> crIndex
                        lfIndex >= 0 -> lfIndex
                        else -> -1
                    }

                    if (endIndex >= 0) {
                        val message = fullMsg.substring(0, endIndex).trim()
                        buffer.delete(0, endIndex + 1)

                        if (message.isNotBlank()) {
                            logPacket(socket.remoteAddress.toString(), message)
                            processMessage(message, deviceId) { ack ->
                                output.writeFully(ack)
                                deviceId = extractDeviceId(message)
                            }
                        }
                        processed++
                    }
                } while (endIndex >= 0)
            }
        }
    } catch (e: Exception) {
        println("[${now()}] Erro na conexão ${socket.remoteAddress}: ${e.message}")
    } finally {
        socket.close()
        println("[${now()}] Conexão encerrada: ${socket.remoteAddress}")
    }
}

private suspend fun processMessage(
    msg: String,
    currentId: String?,
    onAck: suspend (ByteArray) -> Unit
) {
    when {
        msg.startsWith("ST300ALV;") -> {
            val id = msg.substringAfter("ST300ALV;").trim()
            println("[${now()}] HEARTBEAT (ALV) – Device ID: $id")
            val ack = "ST300ACK;$id\r\n".toByteArray(Charsets.US_ASCII)
            onAck(ack)
        }

        msg.startsWith("ST300GPS;") -> {
            if (currentId == null) {
                println("[${now()}] GPS recebido sem ID conhecido. Ignorando.")
                return
            }
            val gps = parseGpsPacket(msg)
            if (gps != null) {
                saveOrUpdateGps(currentId, gps)
                val ack = "ST300ACK;$currentId\r\n".toByteArray(Charsets.US_ASCII)
                onAck(ack)
            }
        }

        msg.startsWith("ST300CMD;") -> {
            println("[${now()}] Comando recebido: $msg")
            val ack = "ST300ACK;${currentId ?: "UNKNOWN"}\r\n".toByteArray(Charsets.US_ASCII)
            onAck(ack)
        }

        else -> {
            println("[${now()}] Pacote desconhecido: $msg")
            val ack = "ST300NAK;UNKNOWN\r\n".toByteArray(Charsets.US_ASCII)
            onAck(ack)
        }
    }
}

private fun logPacket(remote: String, message: String) {
    val hex = message.toByteArray(Charsets.US_ASCII).joinToString(" ") { "%02X".format(it) }
    println(
        """
        |=== PACOTE RECEBIDO ===
        |Remote   : $remote
        |Timestamp: ${now()}
        |Message  : $message
        |Hex      : $hex
        |========================
        """.trimMargin()
    )
}

private fun extractDeviceId(message: String): String? {
    return when {
        message.startsWith("ST300ALV;") -> message.substringAfter("ST300ALV;").trim()
        message.startsWith("ST300GPS;") -> message.substringAfter("ST300GPS;").substringBefore(";")
        else -> null
    }
}

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val ignition: Boolean,
    val timestamp: Long
)

private fun parseGpsPacket(data: String): GpsData? {
    return try {
        // Exemplo real: ST300GPS;807452267;20251031;204446;LAT:-23.5505;LON:-46.6333;SPD:0.0;IGN:0;...
        val parts = data.split(";")
        if (parts.size < 8) return null

        val lat = parts.find { it.startsWith("LAT:") }?.substring(4)?.toDoubleOrNull() ?: 0.0
        val lon = parts.find { it.startsWith("LON:") }?.substring(4)?.toDoubleOrNull() ?: 0.0
        val spd = parts.find { it.startsWith("SPD:") }?.substring(4)?.toDoubleOrNull() ?: 0.0
        val ign = parts.find { it.startsWith("IGN:") }?.substring(4)?.toIntOrNull() == 1
        val ts = System.currentTimeMillis() / 1000L

        GpsData(lat, lon, spd, ign, ts)
    } catch (e: Exception) {
        println("[${now()}] Erro ao parsear GPS: ${e.message}")
        null
    }
}

private fun saveOrUpdateGps(deviceId: String, gps: GpsData) {
    println("[${now()}] GPS [$deviceId] → Lat=${gps.latitude}, Lon=${gps.longitude}, Speed=${gps.speed}, Ign=${gps.ignition}")
    // TODO: salvar no banco
}

private fun now(): String = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
