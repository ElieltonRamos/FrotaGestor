package com.frotagestor.protocols_devices_gps.gt06

import com.frotagestor.interfaces.CommandRequest
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

// ─── Connection Manager ───────────────────────────────────────────────────────
object GT06ConnectionManager {
    private val connections = ConcurrentHashMap<String, Connection>()
    private val mutex = Mutex()

    data class Connection(
        val socket: Socket,
        val writeChannel: ByteWriteChannel
    )

    suspend fun registerConnection(imei: String, socket: Socket) {
        mutex.withLock {
            val writeChannel = socket.openWriteChannel(autoFlush = true)
            connections[imei] = Connection(socket, writeChannel)
            println("[${generateDate()}] ✅ Conexão registrada: IMEI=$imei")
        }
    }

    suspend fun unregisterConnection(imei: String) {
        mutex.withLock {
            connections.remove(imei)?.let { conn ->
                runCatching { conn.writeChannel.close() }
                runCatching { conn.socket.close() }
                println("[${generateDate()}] ❌ Conexão removida: IMEI=$imei")
            }
        }
    }

    suspend fun isDeviceConnected(imei: String): Boolean {
        return mutex.withLock {
            connections[imei]?.let { !it.socket.isClosed } == true
        }
    }

    /** Envia bytes binários diretamente (respostas do protocolo GT06). */
    suspend fun sendRaw(imei: String, bytes: ByteArray): Boolean {
        return mutex.withLock {
            val conn = connections[imei] ?: return@withLock false
            if (conn.socket.isClosed) {
                connections.remove(imei)
                return@withLock false
            }
            try {
                conn.writeChannel.writeFully(bytes)
                println("[${generateDate()}] ✅ Resposta enviada para IMEI=$imei: ${bytes.toHexString()}")
                true
            } catch (e: Exception) {
                println("[${generateDate()}] ❌ Erro ao enviar para IMEI=$imei: ${e.message}")
                connections.remove(imei)
                false
            }
        }
    }

    /** Envia um comando texto encapsulado no pacote 0x80. */
    suspend fun sendCommand(imei: String, command: String, serialNumber: Int = 0): Boolean {
        val packet = buildCommandPacket(command, serialNumber)
        return sendRaw(imei, packet)
    }

    fun getConnectedDevicesCount(): Int = connections.size
    fun getConnectedDevices(): List<String> = connections.keys.toList()
}

// ─── Command Builder ──────────────────────────────────────────────────────────
sealed class BuildCommandResult {
    data class Success(val command: String) : BuildCommandResult()
    data class Error(val message: String) : BuildCommandResult()
}

/**
 * Comandos GT06 são texto ASCII enviados pelo server dentro de um pacote 0x80.
 *
 * Formato do conteúdo do pacote 0x80:
 *   [cmdLength: 2 bytes] [serverFlag: 4 bytes = 0x00000000] [cmdContent: N bytes] [lang: 1 byte = 0x02 (EN)]
 *
 * Exemplos de comandos comuns:
 *   "CUTOFF"       — corta combustível (relay off)
 *   "RESUME"       — restabelece combustível (relay on)
 *   "STATUS"       — solicita status
 *   "RESET"        — reinicia dispositivo
 *   "PARAM"        — solicita parâmetros
 *   "INTERVAL,60"  — define intervalo de envio GPS (segundos)
 *   "APN,<apn>,<user>,<pass>"  — configura APN
 *   "IP,<host>,<port>"         — configura servidor
 */
fun buildGT06CommandText(request: CommandRequest): BuildCommandResult {
    val validCommands = setOf("CUTOFF", "RESUME", "STATUS", "RESET", "PARAM", "INTERVAL", "APN", "IP")

    if (request.commandType !in validCommands) {
        return BuildCommandResult.Error("Comando não suportado: ${request.commandType}")
    }

    val cmd = when (request.commandType) {
        "CUTOFF"   -> "CUTOFF"
        "RESUME"   -> "RESUME"
        "STATUS"   -> "STATUS"
        "RESET"    -> "RESET"
        "PARAM"    -> "PARAM"
        "INTERVAL" -> {
            val seconds = request.parameters["seconds"] ?: "60"
            "INTERVAL,$seconds"
        }
        "APN" -> {
            val apn  = request.parameters["apn"]  ?: return BuildCommandResult.Error("APN ausente")
            val user = request.parameters["user"] ?: ""
            val pass = request.parameters["pass"] ?: ""
            "APN,$apn,$user,$pass"
        }
        "IP" -> {
            val host = request.parameters["host"] ?: return BuildCommandResult.Error("host ausente")
            val port = request.parameters["port"] ?: "5023"
            "IP,$host,$port"
        }
        else -> return BuildCommandResult.Error("Comando inválido")
    }

    return BuildCommandResult.Success(cmd)
}

/**
 * Monta o pacote binário completo 0x80 para envio ao dispositivo.
 *
 * Estrutura:
 *   78 78
 *   [length: 1 byte]   = 1 (protocolNo) + 2 (cmdLen) + 4 (serverFlag) + N (cmd) + 1 (lang) + 2 (serial) + 2 (crc)
 *   80                 (protocol number)
 *   [cmdLength: 2 bytes]
 *   00 00 00 00        (server flag)
 *   [cmdContent: N bytes]
 *   02                 (language: English)
 *   [serial: 2 bytes]
 *   [crc: 2 bytes]
 *   0D 0A
 */
fun buildCommandPacket(command: String, serialNumber: Int): ByteArray {
    val cmdBytes = command.toByteArray(Charsets.US_ASCII)
    val cmdLength = cmdBytes.size

    // Conteúdo do pacote (após protocolNo, antes de serial+crc+stop):
    // cmdLength(2) + serverFlag(4) + cmdBytes(N) + lang(1)
    val contentSize = 2 + 4 + cmdLength + 1

    // length field = protocolNo(1) + content(contentSize) + serial(2) + crc(2) = contentSize + 5 ... mas GT06 conta do protocolNo
    // Na spec: length = protocol_number + information_content + serial_number + error_check = 1 + contentSize + 2 + 2
    val length = 1 + contentSize + 2 + 2

    val serialHi = (serialNumber shr 8) and 0xFF
    val serialLo = serialNumber and 0xFF

    // Monta bytes para CRC (do length até serial)
    val crcInput = ByteArray(1 + 1 + contentSize + 2)
    var idx = 0
    crcInput[idx++] = length.toByte()
    crcInput[idx++] = GT06Protocol.CMD_SERVER.toByte()
    crcInput[idx++] = ((cmdLength shr 8) and 0xFF).toByte()
    crcInput[idx++] = (cmdLength and 0xFF).toByte()
    crcInput[idx++] = 0x00; crcInput[idx++] = 0x00; crcInput[idx++] = 0x00; crcInput[idx++] = 0x00
    cmdBytes.forEach { crcInput[idx++] = it }
    crcInput[idx++] = 0x02  // EN
    crcInput[idx++] = serialHi.toByte()
    crcInput[idx]   = serialLo.toByte()

    val crc = crc16Itu(crcInput)

    // Pacote final
    val packet = ByteArray(2 + 1 + 1 + contentSize + 2 + 2 + 2)
    idx = 0
    packet[idx++] = 0x78.toByte(); packet[idx++] = 0x78.toByte()
    packet[idx++] = length.toByte()
    packet[idx++] = GT06Protocol.CMD_SERVER.toByte()
    packet[idx++] = ((cmdLength shr 8) and 0xFF).toByte()
    packet[idx++] = (cmdLength and 0xFF).toByte()
    packet[idx++] = 0x00; packet[idx++] = 0x00; packet[idx++] = 0x00; packet[idx++] = 0x00
    cmdBytes.forEach { packet[idx++] = it }
    packet[idx++] = 0x02
    packet[idx++] = serialHi.toByte(); packet[idx++] = serialLo.toByte()
    packet[idx++] = ((crc shr 8) and 0xFF).toByte(); packet[idx++] = (crc and 0xFF).toByte()
    packet[idx++] = 0x0D.toByte(); packet[idx] = 0x0A.toByte()

    return packet
}