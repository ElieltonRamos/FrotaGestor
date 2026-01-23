package com.frotagestor.protocols_devices_gps.suntech

import com.frotagestor.services.DailyTripService
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import java.nio.channels.ClosedChannelException
import kotlin.time.Duration.Companion.minutes

private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val dailyTripService = DailyTripService()
private const val portTcp = 1150

@OptIn(DelicateCoroutinesApi::class)
suspend fun startTcpServerSuntech() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", portTcp)

    // Usa o mesmo escopo do servidor (evita GlobalScope)
    dailyTripService.startDailyCleanup(serverScope)

    println("[${generateDate()}] Servidor TCP rodando na porta $portTcp – protocolo Suntech ST310/ST300")

    try {
        while (true) {
            val socket = serverSocket.accept()
            val remote = runCatching { socket.remoteAddress.toString() }
                .getOrElse { "UNKNOWN" }

            println("[${generateDate()}] Nova conexão: $remote")

            serverScope.launch {
                handleDevice(socket, remote)
            }
        }
    } catch (e: Exception) {
        println("[${generateDate()}] Erro no servidor TCP: ${e.message}")
    } finally {
        println("[${generateDate()}] Encerrando servidor TCP Suntech...")
        serverScope.cancel()
        serverSocket.close()
    }
}

fun logPacket(remote: String, message: String) {
    println(
        """
        |=== PACOTE RECEBIDO ===
        |Remote   : $remote
        |Timestamp: ${generateDate()}
        |Message  : $message
        |========================
        """.trimMargin()
    )
}

suspend fun handleDevice(socket: Socket, remote: String) {
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

                while (true) {
                    val fullMsg = buffer.toString()
                    val endIndex =
                        fullMsg.indexOf('\r').takeIf { it >= 0 }
                            ?: fullMsg.indexOf('\n').takeIf { it >= 0 }
                            ?: -1

                    if (endIndex < 0) break

                    val message = fullMsg.substring(0, endIndex).trim()
                    buffer.delete(0, endIndex + 1)

                    if (message.isBlank()) continue

                    logPacket(remote, message)

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
            }
        }
    } catch (e: ClosedChannelException) {
        // Comportamento normal: rastreador fechou o socket
    } catch (e: Exception) {
        println("[${generateDate()}] Erro na conexão $remote: ${e.message}")
    } finally {
        if (deviceId != null) {
            DeviceConnectionManager.unregisterConnection(deviceId!!)
            println("[${generateDate()}] Conexão removida: ID=$deviceId")
        }

        runCatching { socket.close() }

        println("[${generateDate()}] Conexão encerrada: $remote")
    }
}
