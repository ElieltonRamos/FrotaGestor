package com.frotagestor.protocols_devices_gps.suntech

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes

private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

suspend fun startTcpServerSuntech() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", 1150)

    println("[${generateDate()}] Servidor TCP rodando na porta 1150 – protocolo Suntech ST310/ST300")

    try {
        while (true) {
            val socket = serverSocket.accept()
            println("[${generateDate()}] Nova conexão: ${socket.remoteAddress}")
            serverScope.launch { handleDevice(socket) }
        }
    } catch (e: Exception) {
        println("[${generateDate()}] Erro no servidor: ${e.message}")
    } finally {
        println("[${generateDate()}] Encerrando servidor TCP Suntech...")
        serverScope.cancel()
        serverSocket.close()
    }
}

suspend fun handleDevice(socket: Socket) {
    val input = socket.openReadChannel()
    var deviceId: String? = null
    val buffer = StringBuilder()

    try {
        withTimeoutOrNull(30.minutes) {
            while (!input.isClosedForRead) {
                val bytes = ByteArray(1024)
                val read = input.readAvailable(bytes)
                if (read <= 0) continue

                val ascii = bytes.copyOf(read).toString(Charsets.US_ASCII)
                buffer.append(ascii)

                do {
                    val fullMsg = buffer.toString()
                    val endIndex = fullMsg.indexOf('\r').takeIf { it >= 0 } ?: fullMsg.indexOf('\n').takeIf { it >= 0 } ?: -1
                    if (endIndex < 0) break

                    val message = fullMsg.substring(0, endIndex).trim()
                    buffer.delete(0, endIndex + 1)

                    if (message.isNotBlank()) {
                        logPacket(socket.remoteAddress.toString(), message)

                        val extractedId = extractDeviceId(message)
                        if (extractedId != null && deviceId == null) {
                            deviceId = extractedId
                            DeviceConnectionManager.registerConnection(deviceId!!, socket)
                            println("[${generateDate()}] Dispositivo registrado: DeviceID=$deviceId")
                        }

                        processMessage(message, deviceId) { imei, command ->
                            DeviceConnectionManager.sendCommand(imei, "$command\r")
                        }
                    }
                } while (true)
            }
        }
    } catch (e: Exception) {
        println("[${generateDate()}] Erro na conexão ${socket.remoteAddress}: ${e.message}")
    } finally {
        if (deviceId != null) {
            DeviceConnectionManager.unregisterConnection(deviceId!!)
        }
        socket.close()
        println("[${generateDate()}] Conexão encerrada: ${socket.remoteAddress}")
    }
}