package com.frotagestor.protocols_devices_gps.suntech

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes

private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

suspend fun startTcpServerSuntech() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", 3002)

    println("[${generateDate()}] Servidor TCP rodando na porta 3002 – protocolo Suntech ST310/ST300")

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
    val output = socket.openWriteChannel(autoFlush = true)
    var deviceId: String? = null
    val buffer = StringBuilder()

    try {
        withTimeoutOrNull(5.minutes) {
            while (!input.isClosedForRead) {
                val bytes = ByteArray(1024)
                val read = input.readAvailable(bytes)

                if (read > 0) {
                    val rawBytes = bytes.copyOf(read)
                    val ascii = rawBytes.toString(Charsets.US_ASCII)
                    buffer.append(ascii)

                    do {
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

                                deviceId = extractDeviceId(message) ?: deviceId

                                processMessage(message, deviceId) { ack ->
                                    output.writeFully(ack)
                                }
                            }
                        }
                    } while (endIndex >= 0)
                }
            }
        } ?: println("[${generateDate()}] Timeout de leitura – desconectando ${socket.remoteAddress}")

    } catch (e: Exception) {
        println("[${generateDate()}] Erro na conexão ${socket.remoteAddress}: ${e.message}")
    } finally {
        socket.close()
        println("[${generateDate()}] Conexão encerrada: ${socket.remoteAddress}")
    }
}
