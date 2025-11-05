package com.frotagestor.protocols_devices_gps.suntech

import com.frotagestor.interfaces.CommandRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully

object DeviceConnectionManager {
    private val connections = ConcurrentHashMap<String, Connection>()
    private val mutex = Mutex()

    data class Connection(
        val socket: Socket,
        val writeChannel: ByteWriteChannel
    )

    suspend fun registerConnection(deviceId: String, socket: Socket) {
        mutex.withLock {
            val writeChannel = socket.openWriteChannel(autoFlush = true)
            connections[deviceId] = Connection(socket, writeChannel)
            println("[${generateDate()}] ✅ Conexão registrada: ID=$deviceId")
        }
    }

    suspend fun unregisterConnection(deviceId: String) {
        mutex.withLock {
            connections.remove(deviceId)?.let { conn ->
                try { conn.writeChannel.close() } catch (e: Exception) { /* ignore */ }
                try { conn.socket.close() } catch (e: Exception) { /* ignore */ }
                println("[${generateDate()}] ❌ Conexão removida: ID=$deviceId")
            }
        }
    }

    suspend fun isDeviceConnected(deviceId: String): Boolean {
        return mutex.withLock {
            connections[deviceId]?.let { conn ->
                !conn.socket.isClosed
            } == true
        }
    }

    suspend fun sendCommand(deviceId: String, command: String): Boolean {
        return mutex.withLock {
            val conn = connections[deviceId] ?: return@withLock false
            if (conn.socket.isClosed) {
                connections.remove(deviceId)
                println("[${generateDate()}] ❌ Socket fechado para ID: $deviceId")
                return@withLock false
            }
            try {
                // ✅ CORREÇÃO: Adiciona \r no final do comando
                val fullCommand = if (command.endsWith("\r")) command else "$command\r"
                conn.writeChannel.writeFully(fullCommand.toByteArray(Charsets.US_ASCII))
                println("[${generateDate()}] ✅ Comando enviado para $deviceId: $command")
                true
            } catch (e: Exception) {
                println("[${generateDate()}] ❌ Erro ao enviar comando para $deviceId: ${e.message}")
                unregisterConnection(deviceId)
                false
            }
        }
    }

    fun getConnectedDevicesCount(): Int {
        return connections.size
    }

    fun getConnectedDevices(): List<String> {
        return connections.keys.toList()
    }
}

sealed class BuildCommandResult {
    data class Success(val command: String) : BuildCommandResult()
    data class Error(val message: String) : BuildCommandResult()
}

fun buildSuntechCommand(deviceId: String, request: CommandRequest): BuildCommandResult {
    // Lista de comandos permitidos
    val validCommands = setOf(
        "StatusReq", "Enable1", "Disable1", "Enable2", "Disable2",
        "OUT", "RPT", "NTW", "EVT", "SVC", "ATH", "ParamReq"
    )

    if (request.commandType !in validCommands) {
        return BuildCommandResult.Error("Comando não suportado: ${request.commandType}")
    }

    val cmd = when (request.commandType) {
        "StatusReq" -> "ST300CMD;$deviceId;02;StatusReq"
        "Enable1" -> "ST300CMD;$deviceId;02;Enable1"
        "Disable1" -> "ST300CMD;$deviceId;02;Disable1"
        "OUT" -> {
            val output = request.parameters["output"] ?: "1"
            val enable = request.parameters["enable"]?.toBoolean() ?: false
            val action = if (enable) "Enable$output" else "Disable$output"
            "ST300CMD;$deviceId;02;$action"
        }

        "RPT" -> {
            val driving = request.parameters["driving"] ?: "60"
            val parking = request.parameters["parking"] ?: "300"
            val idle = request.parameters["idle"] ?: "120"
            val attempts = request.parameters["attempts"] ?: "3"
            val distance = request.parameters["distance"] ?: "0"
            "ST300RPT;$deviceId;02;$driving;$parking;$idle;$attempts;$distance;0;0;0;0"
        }

        "NTW" -> {
            val authMode = request.parameters["authMode"] ?: "0"
            val apn = request.parameters["apn"] ?: "internet"
            val user = request.parameters["user"] ?: ""
            val pass = request.parameters["pass"] ?: ""
            val serverIp = request.parameters["serverIp"] ?: ""
            val serverPort = request.parameters["serverPort"] ?: "3002"
            val backupIp = request.parameters["backupIp"] ?: ""
            val backupPort = request.parameters["backupPort"] ?: "3002"
            val sleep = request.parameters["sleep"] ?: "0"
            "ST300NTW;$deviceId;02;$authMode;$apn;$user;$pass;$serverIp;$serverPort;$backupIp;$backupPort;$sleep"
        }

        "EVT" -> {
            val ignitionAlert = request.parameters["ignitionAlert"] ?: "1"
            val ignitionDelay = request.parameters["ignitionDelay"] ?: "60"
            val sleepMode = request.parameters["sleepMode"] ?: "0"
            val in1Type = request.parameters["in1Type"] ?: "3"
            val in2Type = request.parameters["in2Type"] ?: "2"
            val in3Type = request.parameters["in3Type"] ?: "2"
            "ST300EVT;$deviceId;02;$ignitionAlert;$ignitionDelay;$sleepMode;$in1Type;$in2Type;$in3Type;30;20;20;1;0;1;0;0;0;0;0;0;0;0;0;0;0;0"
        }

        "SVC" -> {
            val parkingLock = request.parameters["parkingLock"] ?: "1"
            val speedLimit = request.parameters["speedLimit"] ?: "120"
            val speedLimitTime = request.parameters["speedLimitTime"] ?: "10"
            "ST300SVC;$deviceId;02;$parkingLock;$speedLimit;$speedLimitTime;0;0;0;1;1;1;0;0;0;0"
        }

        "ATH" -> {
            val enable = request.parameters["enable"]?.toBoolean() ?: true
            val sensitivity = request.parameters["sensitivity"] ?: "5"
            "ST300ATH;$deviceId;02;${if (enable) 1 else 0};$sensitivity;30;3"
        }
        else -> return BuildCommandResult.Error("Comando inválido") // nunca chega aqui
    }

    return BuildCommandResult.Success(cmd)
}