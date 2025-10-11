package com.frotagestor.accurate_gt_06

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.*
import parseImeiFromLogin
import parseGpsPacket
import saveOrUpdateGps

suspend fun startTcpServer() {
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .bind("0.0.0.0", 5000)

    println("Servidor TCP rodando na porta 5000")

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
                // 📌 Print do pacote recebido em hexadecimal
                println("Pacote recebido (${bytesRead} bytes): " + data.joinToString(" ") { "%02X".format(it) })
                val packetType = data[3].toInt() and 0xFF

                when (packetType) {
                    0x01 -> { // LOGIN
                        imei = parseImeiFromLogin(data)
                        println("Dispositivo conectado: IMEI=$imei")

                        // Envia ACK de login
                        val ackLogin = byteArrayOf(
                            0x78, 0x78, 0x05, 0x01, 0x00, 0x00, 0x0D, 0x0A
                        )
                        output.writeFully(ackLogin)
                    }

                    0x12, 0x22 -> { // GPS fix / status
                        if (imei == null) continue // ignorar se login não recebido
                        val gps = parseGpsPacket(data)
                        saveOrUpdateGps(imei, gps)

                        // ACK para GPS
                        val serial = data[1] // número de série do pacote
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
