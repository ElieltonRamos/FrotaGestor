package com.frotagestor.protocols_devices_gps.gt06

import com.frotagestor.services.DailyTripService
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import java.nio.channels.ClosedChannelException
import kotlin.time.Duration.Companion.minutes

private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val dailyTripService = DailyTripService()
private const val portTcp = 3002

@OptIn(DelicateCoroutinesApi::class)
suspend fun startTcpServerGT06() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", portTcp)

    dailyTripService.startDailyCleanup(serverScope)

    println("[${generateDate()}] Servidor TCP rodando na porta $portTcp – protocolo GT06 (LT32 Pro)")

    try {
        while (true) {
            val socket = serverSocket.accept()
            val remote = runCatching { socket.remoteAddress.toString() }.getOrElse { "UNKNOWN" }

            println("[${generateDate()}] Nova conexão: $remote")

            serverScope.launch {
                handleDeviceGT06(socket, remote)
            }
        }
    } catch (e: Exception) {
        println("[${generateDate()}] Erro no servidor TCP: ${e.message}")
    } finally {
        println("[${generateDate()}] Encerrando servidor TCP GT06...")
        serverScope.cancel()
        serverSocket.close()
    }
}

suspend fun handleDeviceGT06(socket: Socket, remote: String) {
    val input = socket.openReadChannel()
    var imei: String? = null

    // Buffer de bytes — GT06 é binário, não tem delimitador de texto
    val buffer = mutableListOf<Byte>()

    try {
        withTimeoutOrNull(30.minutes) {
            while (!input.isClosedForRead) {
                val bytes = ByteArray(1024)
                val read = input.readAvailable(bytes)

                if (read <= 0) continue

                buffer.addAll(bytes.take(read))

                // Processa todos os pacotes completos no buffer
                while (true) {
                    val packet = extractNextPacket(buffer) ?: break

                    logPacketGT06(remote, packet)

                    val result = processPacketGT06(packet, imei)

                    // Registra IMEI no primeiro login
                    if (imei == null && result.imei != null) {
                        imei = result.imei
                        GT06ConnectionManager.registerConnection(imei!!, socket)
                        println("[${generateDate()}] Dispositivo registrado: IMEI=$imei")
                    }

                    // Envia resposta ao dispositivo se necessário
                    result.response?.let { responseBytes ->
                        GT06ConnectionManager.sendRaw(imei ?: "", responseBytes)
                    }
                }
            }
        }
    } catch (e: ClosedChannelException) {
        // comportamento normal
    } catch (e: Exception) {
        println("[${generateDate()}] Erro na conexão $remote: ${e.message}")
    } finally {
        if (imei != null) {
            GT06ConnectionManager.unregisterConnection(imei!!)
            println("[${generateDate()}] Conexão removida: IMEI=$imei")
        }
        runCatching { socket.close() }
        println("[${generateDate()}] Conexão encerrada: $remote")
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
    // Procura o start bit 0x78 0x78
    var startIndex = -1
    for (i in 0 until buffer.size - 1) {
        if (buffer[i] == 0x78.toByte() && buffer[i + 1] == 0x78.toByte()) {
            startIndex = i
            break
        }
    }

    if (startIndex < 0) {
        // Nenhum start bit — descarta tudo
        buffer.clear()
        return null
    }

    // Descarta lixo antes do start bit
    if (startIndex > 0) {
        repeat(startIndex) { buffer.removeAt(0) }
    }

    // Precisa de pelo menos 5 bytes para ler o length
    // [0x78][0x78][length][protocolNo]...[0x0D][0x0A]
    if (buffer.size < 5) return null

    val packetLength = buffer[2].toInt() and 0xFF
    // Tamanho total = 2 (start) + 1 (length) + packetLength + 2 (stop)
    val totalSize = 2 + 1 + packetLength + 2

    if (buffer.size < totalSize) return null

    val packet = ByteArray(totalSize) { buffer[it] }

    // Valida stop bit
    if (packet[totalSize - 2] != 0x0D.toByte() || packet[totalSize - 1] != 0x0A.toByte()) {
        // Pacote corrompido — descarta apenas o start bit e tenta novamente
        buffer.removeAt(0)
        buffer.removeAt(0)
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
        |Timestamp: ${generateDate()}
        |Hex      : ${packet.toHexString()}
        |============================
        """.trimMargin()
    )
}

fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }