package com.frotagestor.accurate_gt_06

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.*

suspend fun startTcpServerGt06() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", 5023)
    println("Servidor TCP rodando na porta 5023 para protocolo GT-06")

    while (true) {
        val socket = serverSocket.accept()
        println("Nova conexão: ${socket.remoteAddress}")
        GlobalScope.launch { handleDevice(socket) }
    }
}

suspend fun handleDevice(socket: Socket) {
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)
    var imei: String? = null
    try {
        while (!input.isClosedForRead) {
            val buffer = ByteArray(1024)
            val bytesRead = input.readAvailable(buffer)
            if (bytesRead > 0) {
                val data = buffer.copyOf(bytesRead)
                println("Pacote recebido (${bytesRead} bytes): " + data.joinToString(" ") { "%02X".format(it) })
                val packetType = data[3].toInt() and 0xFF
                when (packetType) {
                    0x01 -> {
                        imei = parseImeiFromLogin(data)
                        println("Dispositivo conectado: IMEI=$imei")
                        val ackLogin = byteArrayOf(
                            0x78, 0x78, 0x05, 0x01, 0x00, 0x00, 0x0D, 0x0A
                        )
                        output.writeFully(ackLogin)
                    }

                    0x12, 0x22 -> {
                        if (imei == null) continue
                        val gps = parseGpsPacket(data)
                        saveOrUpdateGps(imei, gps!!)
                        val serial = data[1]
                        val ackGps = byteArrayOf(
                            0x78, 0x78, 0x05, packetType.toByte(), serial, 0x00, 0x0D, 0x0A
                        )
                        output.writeFully(ackGps)
                    }

                    else -> println("Pacote desconhecido: tipo=$packetType")
                }
            }
        }
    } catch (e: Exception) {
        println("Erro na conexão TCP: ${e.message}")
    } finally {
        socket.close()
        println("Conexão encerrada: ${socket.remoteAddress}")
    }
}
