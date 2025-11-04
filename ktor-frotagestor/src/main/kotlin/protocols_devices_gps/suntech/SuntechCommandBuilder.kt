package com.frotagestor.protocols_devices_gps.suntech

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

    suspend fun registerConnection(imei: String, socket: Socket) {
        mutex.withLock {
            val writeChannel = socket.openWriteChannel(autoFlush = true)
            connections[imei] = Connection(socket, writeChannel)
        }
    }

    suspend fun unregisterConnection(imei: String) {
        mutex.withLock {
            connections.remove(imei)?.let { conn ->
                try { conn.writeChannel.close() } catch (e: Exception) { /* ignore */ }
                try { conn.socket.close() } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    suspend fun getConnection(imei: String): Connection? {
        return mutex.withLock { connections[imei] }
    }

    suspend fun isDeviceConnected(imei: String): Boolean {
        return mutex.withLock {
            connections[imei]?.let { conn ->
                !conn.socket.isClosed
            } == true
        }
    }

    suspend fun sendCommand(imei: String, command: String): Boolean {
        return mutex.withLock {
            val conn = connections[imei] ?: return@withLock false
            if (conn.socket.isClosed) {
                connections.remove(imei)
                return@withLock false
            }
            try {
                conn.writeChannel.writeFully("$command\r".toByteArray(Charsets.US_ASCII))
                true
            } catch (e: Exception) {
                println("Erro ao enviar comando para $imei: ${e.message}")
                unregisterConnection(imei)
                false
            }
        }
    }
}