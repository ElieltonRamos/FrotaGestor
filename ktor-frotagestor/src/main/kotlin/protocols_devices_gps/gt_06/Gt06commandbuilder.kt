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
    private data class Connection(
        val socket:       Socket,
        val writeChannel: ByteWriteChannel
    )

    private val connections = ConcurrentHashMap<String, Connection>()
    private val mutex       = Mutex()

    suspend fun registerConnection(imei: String, socket: Socket) {
        mutex.withLock {
            connections[imei] = Connection(socket, socket.openWriteChannel(autoFlush = true))
            println("[${now()}] ✅ Conexão registrada: IMEI=$imei")
        }
    }

    suspend fun unregisterConnection(imei: String) {
        mutex.withLock {
            connections.remove(imei)?.let { conn ->
                runCatching { conn.writeChannel.close() }
                runCatching { conn.socket.close() }
                println("[${now()}] ❌ Conexão removida: IMEI=$imei")
            }
        }
    }

    suspend fun isDeviceConnected(imei: String): Boolean =
        mutex.withLock { connections[imei]?.let { !it.socket.isClosed } == true }

    /** Envia bytes binários diretamente (respostas do protocolo GT06). */
    suspend fun sendRaw(imei: String, bytes: ByteArray): Boolean =
        mutex.withLock {
            val conn = connections[imei] ?: return@withLock false
            if (conn.socket.isClosed) {
                connections.remove(imei)
                return@withLock false
            }
            try {
                conn.writeChannel.writeFully(bytes)
                println("[${now()}] ✅ Resposta enviada para IMEI=$imei: ${bytes.toHexString()}")
                true
            } catch (e: Exception) {
                println("[${now()}] ❌ Erro ao enviar para IMEI=$imei: ${e.message}")
                connections.remove(imei)
                false
            }
        }

    /** Envia um comando texto encapsulado no pacote 0x80. */
    suspend fun sendCommand(imei: String, command: String, serialNumber: Int = 0): Boolean =
        sendRaw(imei, buildCommandPacket(command, serialNumber))

    fun getConnectedDevicesCount(): Int  = connections.size
    fun getConnectedDevices(): List<String> = connections.keys.toList()
}

// ─── Command Builder ──────────────────────────────────────────────────────────
sealed class BuildCommandResult {
    data class Success(val command: String) : BuildCommandResult()
    data class Error(val message: String)   : BuildCommandResult()
}

/**
 * Comandos GT06 são texto ASCII enviados pelo server dentro de um pacote 0x80.
 *
 * Formato do conteúdo do pacote 0x80:
 *   [cmdLength: 2 bytes] [serverFlag: 4 bytes = 0x00000000] [cmdContent: N bytes] [lang: 1 byte = 0x02 (EN)]
 *
 * Exemplos de comandos comuns:
 *   "CUTOFF"                    — corta combustível (relay off)
 *   "RESUME"                    — restabelece combustível (relay on)
 *   "STATUS"                    — solicita status
 *   "RESET"                     — reinicia dispositivo
 *   "PARAM"                     — solicita parâmetros
 *   "INTERVAL,60"               — define intervalo de envio GPS (segundos)
 *   "APN,<apn>,<user>,<pass>"   — configura APN
 *   "IP,<host>,<port>"          — configura servidor
 */
private val suntechToGT06 = mapOf(
    "StatusReq" to "STATUS",
    "Enable1"   to "CUTOFF",
    "Disable1"  to "RESUME"
)

fun buildGT06CommandText(request: CommandRequest): BuildCommandResult {
    val commandType = suntechToGT06[request.commandType] ?: request.commandType

    val validCommands = setOf("CUTOFF", "RESUME", "STATUS", "RESET", "PARAM", "INTERVAL", "APN", "IP")

    if (commandType !in validCommands)
        return BuildCommandResult.Error("Comando não suportado: ${request.commandType}")

    val cmd = when (commandType) {
        "CUTOFF", "RESUME", "STATUS", "RESET", "PARAM" -> commandType
        "INTERVAL" -> "INTERVAL,${request.parameters["seconds"] ?: "60"}"
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
 *   [length: 1]        = 1 (protocolNo) + 2 (cmdLen) + 4 (serverFlag) + N (cmd) + 1 (lang) + 2 (serial) + 2 (crc)
 *   80                 (protocol number)
 *   [cmdLength: 2]
 *   00 00 00 00        (server flag)
 *   [cmdContent: N]
 *   02                 (language: English)
 *   [serial: 2]
 *   [crc: 2]
 *   0D 0A
 */
fun buildCommandPacket(command: String, serialNumber: Int): ByteArray {
    val cmdBytes    = command.toByteArray(Charsets.US_ASCII)
    val cmdLength   = cmdBytes.size
    val contentSize = 2 + 4 + cmdLength + 1                 // cmdLen + serverFlag + cmd + lang
    val length      = 1 + contentSize + 2 + 2               // protocolNo + content + serial + crc

    val serialHi = (serialNumber shr 8) and 0xFF
    val serialLo =  serialNumber        and 0xFF

    fun Int.hi() = ((this shr 8) and 0xFF).toByte()
    fun Int.lo() = (this and 0xFF).toByte()

    val body = buildList<Byte> {
        add(length.toByte())
        add(GT06Protocol.CMD_SERVER.toByte())
        add(cmdLength.hi()); add(cmdLength.lo())
        repeat(4) { add(0x00) }                             // server flag
        addAll(cmdBytes.toList())
        add(0x02)                                           // lang EN
        add(serialHi.toByte()); add(serialLo.toByte())
    }.toByteArray()

    val crc = crc16Itu(body)

    return buildList<Byte> {
        add(0x78.toByte()); add(0x78.toByte())
        addAll(body.toList())
        add(crc.hi()); add(crc.lo())
        add(0x0D.toByte()); add(0x0A.toByte())
    }.toByteArray()
}