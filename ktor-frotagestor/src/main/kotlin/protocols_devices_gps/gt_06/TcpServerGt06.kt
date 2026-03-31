package com.frotagestor.protocols_devices_gps.gt06

import com.frotagestor.services.DailyTripService
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import java.nio.channels.ClosedChannelException
import kotlin.time.Duration.Companion.minutes

private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private const val PORT_TCP = 3002

suspend fun startTcpServerGT06(dailyTripService: DailyTripService) {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", PORT_TCP)

    println("[${now()}] Servidor TCP rodando na porta $PORT_TCP – protocolo GT06 (LT32 Pro)")

    try {
        while (true) {
            val socket = serverSocket.accept()
            val remote = runCatching { socket.remoteAddress.toString() }.getOrElse { "UNKNOWN" }

            println("[${now()}] Nova conexão: $remote")

            serverScope.launch {
                handleDeviceGT06(socket, remote, dailyTripService)
            }
        }
    } catch (e: Exception) {
        println("[${now()}] Erro no servidor TCP: ${e.message}")
    } finally {
        println("[${now()}] Encerrando servidor TCP GT06...")
        serverScope.cancel()
        serverSocket.close()
    }
}

suspend fun handleDeviceGT06(socket: Socket, remote: String, dailyTripService: DailyTripService) {
    val input  = socket.openReadChannel()
    val buffer = mutableListOf<Byte>()
    var imei: String? = null

    try {
        withTimeoutOrNull(30.minutes) {
            while (!input.isClosedForRead) {
                val bytes = ByteArray(1024)
                val read  = input.readAvailable(bytes)

                if (read <= 0) continue

                buffer.addAll(bytes.take(read))

                while (true) {
                    val packet = extractNextPacket(buffer) ?: break

                    logPacketGT06(remote, packet)

                    val result = processPacketGT06(packet, imei, dailyTripService)

                    if (imei == null && result.imei != null) {
                        imei = result.imei
                        GT06ConnectionManager.registerConnection(imei!!, socket)
                        println("[${now()}] Dispositivo registrado: IMEI=$imei")
                    }

                    result.response?.let { GT06ConnectionManager.sendRaw(imei ?: "", it) }
                }
            }
        }
    } catch (e: ClosedChannelException) {
        // comportamento normal
    } catch (e: Exception) {
        println("[${now()}] Erro na conexão $remote: ${e.message}")
    } finally {
        if (imei != null) {
            GT06ConnectionManager.unregisterConnection(imei!!)
            println("[${now()}] Conexão removida: IMEI=$imei")
        }
        runCatching { socket.close() }
        println("[${now()}] Conexão encerrada: $remote")
    }
}

/**
 * Tenta extrair um pacote GT06 completo do buffer.
 * Formato: 0x78 0x78 [length:1] [protocolNo:1] [content:N] [serialNo:2] [crc:2] 0x0D 0x0A
 *
 * Remove os bytes consumidos do buffer.
 * Retorna null se ainda não há pacote completo.
 */
fun extractNextPacket(buffer: MutableList<Byte>): ByteArray? {
    val startIndex = (0 until buffer.size - 1)
        .firstOrNull { buffer[it] == 0x78.toByte() && buffer[it + 1] == 0x78.toByte() }

    if (startIndex == null) {
        buffer.clear()
        return null
    }

    if (startIndex > 0) repeat(startIndex) { buffer.removeAt(0) }

    if (buffer.size < 5) return null

    val packetLength = buffer[2].toInt() and 0xFF
    val totalSize    = 2 + 1 + packetLength + 2

    if (buffer.size < totalSize) return null

    val packet = ByteArray(totalSize) { buffer[it] }

    if (packet[totalSize - 2] != 0x0D.toByte() || packet[totalSize - 1] != 0x0A.toByte()) {
        repeat(2) { buffer.removeAt(0) }
        return null
    }

    repeat(totalSize) { buffer.removeAt(0) }

    return packet
}

fun logPacketGT06(remote: String, packet: ByteArray) {
    println(
        """
        |=== PACOTE GT06 RECEBIDO ===
        |Remote   : $remote
        |Timestamp: ${now()}
        |Hex      : ${packet.toHexString()}
        |============================
        """.trimMargin()
    )
}

fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }