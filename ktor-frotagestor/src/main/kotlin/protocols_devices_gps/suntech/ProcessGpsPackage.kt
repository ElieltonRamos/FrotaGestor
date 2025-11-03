package com.frotagestor.protocols_devices_gps.suntech

import com.frotagestor.accurate_gt_06.findVehicleIdByImei
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

// Dados GPS básicos
data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val ignition: Boolean,
    val dateTime: LocalDateTime,
)

suspend fun processMessage(
    msg: String,
    currentId: String?,
    onAck: suspend (ByteArray) -> Unit
) {
    when {
        // === HEARTBEAT / ALIVE (mantém conexão ativa) ===
        msg.startsWith("ST300ALV;") -> {
            val id = msg.substringAfter("ST300ALV;").trim()
            println("[${generateDate()}] HEARTBEAT (ALV) – Device ID: $id")
            val ack = "ST300ACK;$id\r\n".toByteArray(Charsets.US_ASCII)
            onAck(ack)
        }

        // === PACOTE DE POSIÇÃO NORMAL ===
        msg.startsWith("ST300GPS;") || msg.startsWith("ST300STT;") -> {
            if (currentId == null) {
                val id = extractDeviceId(msg)
                if (id != null) {
                    val gps = parseGpsPacket(msg)
                    if (gps != null) {
                        saveOrUpdateGps(id, gps, msg) // Passa a mensagem raw
                        println("[${generateDate()}] 📍 Posição STT – ID: $id")
                        val ack = "ST300ACK;$id\r\n".toByteArray(Charsets.US_ASCII)
                        onAck(ack)
                        return
                    }
                }
                println("[${generateDate()}] GPS recebido sem ID conhecido. Ignorando.")
                return
            }
            val gps = parseGpsPacket(msg)
            if (gps != null) {
                saveOrUpdateGps(currentId, gps, msg) // Passa a mensagem raw
                println("[${generateDate()}] 📍 Posição STT – ID: $currentId")
                val ack = "ST300ACK;$currentId\r\n".toByteArray(Charsets.US_ASCII)
                onAck(ack)
            }
        }

        // === PACOTE DE COMANDO (resposta a comandos enviados ao dispositivo) ===
        msg.startsWith("ST300CMD;") -> {
            println("[${generateDate()}] Comando recebido: $msg")
            val ack = "ST300ACK;${currentId ?: "UNKNOWN"}\r\n".toByteArray(Charsets.US_ASCII)
            onAck(ack)
        }

        // === ALERTA (ALT) – indica eventos automáticos do dispositivo ===
        msg.startsWith("ST300ALT;") -> {
            val id = extractDeviceId(msg) ?: currentId
            val gps = parseGpsPacket(msg)
            if (gps != null && id != null) {
                saveOrUpdateGps(id, gps, msg) // Passa a mensagem raw

                val eventCode = msg.split(";").getOrNull(16)?.toIntOrNull()
                val eventDescription = when (eventCode) {
                    40 -> "Ignição LIGADA (tensão detectada na Entrada 1)"
                    41 -> "Ignição DESLIGADA (sem tensão na Entrada 1)"
                    3 -> "Bateria principal desconectada"
                    else -> "Evento detectado pelo dispositivo"
                }

                println("[${generateDate()}] ALERTA (ALT) – ID: $id – $eventDescription")
                val ack = "ST300ACK;$id\r\n".toByteArray(Charsets.US_ASCII)
                onAck(ack)
            }
        }

        // === EMERGÊNCIA (EMG) – indica acionamento manual de pânico/SOS ===
        msg.startsWith("ST300EMG;") -> {
            val id = extractDeviceId(msg) ?: currentId
            val gps = parseGpsPacket(msg)
            if (gps != null && id != null) {
                saveOrUpdateGps(id, gps, msg) // Passa a mensagem raw

                val eventCode = msg.split(";").getOrNull(16)?.toIntOrNull()
                val emergencyType = when (eventCode) {
                    3 -> "Bateria principal desconectada"
                    7 -> "Movimento/Choque detectado"
                    else -> "Emergência acionada"
                }

                println("[${generateDate()}] EMERGÊNCIA (EMG) – ID: $id – $emergencyType")
                val ack = "ST300ACK;$id\r\n".toByteArray(Charsets.US_ASCII)
                onAck(ack)
            }
        }

        // === PACOTE DESCONHECIDO ===
        else -> {
            println("[${generateDate()}] Pacote desconhecido: $msg")
            val ack = "ST300NAK;UNKNOWN\r\n".toByteArray(Charsets.US_ASCII)
            onAck(ack)
        }
    }
}

fun logPacket(remote: String, message: String) {
    val hex = message.toByteArray(Charsets.US_ASCII).joinToString(" ") { "%02X".format(it) }
    println(
        """
        |=== PACOTE RECEBIDO ===
        |Remote   : $remote
        |Timestamp: ${generateDate()}
        |Message  : $message
        |Hex      : $hex
        |========================
        """.trimMargin()
    )
}

fun extractDeviceId(message: String): String? {
    return when {
        message.startsWith("ST300ALV;") -> message.substringAfter("ST300ALV;").substringBefore(";").trim()
        message.startsWith("ST300GPS;") -> message.substringAfter("ST300GPS;").substringBefore(";").trim()
        message.startsWith("ST300STT;") -> message.substringAfter("ST300STT;").substringBefore(";").trim()
        message.startsWith("ST300ALT;") -> message.substringAfter("ST300ALT;").substringBefore(";").trim()
        message.startsWith("ST300EMG;") -> message.substringAfter("ST300EMG;").substringBefore(";").trim()
        else -> null
    }
}

fun parseGpsPacket(data: String): GpsData? {
    return try {
        val parts = data.split(";")

        // Detecta o tipo de pacote
        val packetType = parts.getOrNull(0) ?: ""

        // Exemplo de pacote ALT/EMG:
        // ST300ALT;807452267;145;407;20251101;13:10:37;8f8218;-14.949052;-042.840349;000.000;109.50;4;1;46894505;11.93;000000;40;080059;4.0;1
        // [0]      [1]       [2] [3] [4]      [5]      [6]    [7]        [8]         [9]      [10]   [11][12][13]     [14]  [15]   [16][17]  [18] [19]

        // Exemplo de pacote STT:
        // ST300STT;807452267;145;407;20251101;13:33:49;8f8244;-14.948942;-042.840272;001.037;007.98;9;1;46894544;0.00;000000;1;0008;080059;4.1;1
        // [0]      [1]       [2] [3] [4]      [5]      [6]    [7]        [8]         [9]      [10]   [11][12][13]     [14]  [15]   [16][17][18]  [19][20]

        val latitude = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0
        val longitude = parts.getOrNull(8)?.toDoubleOrNull() ?: 0.0
        val speed = parts.getOrNull(9)?.toDoubleOrNull() ?: 0.0
        val heading = parts.getOrNull(10)?.toDoubleOrNull() ?: 0.0

        // 🔍 ANÁLISE DA IGNIÇÃO:
        // Para pacotes ALT/EMG: Campo [16] contém o código do evento
        // Para pacotes STT: Campo [16] é diferente, usar último campo
        val eventCode = if (packetType.contains("ALT") || packetType.contains("EMG")) {
            parts.getOrNull(16)?.toIntOrNull()
        } else {
            null
        }

        // Campo último sempre contém status I/O
        val ioStatus = parts.lastOrNull()?.toIntOrNull()

        // Determina ignição baseado no código do evento (mais confiável)
        val ignition = when (eventCode) {
            40 -> true   // Evento de ignição ligada
            41 -> false  // Evento de ignição desligada
            3 -> false   // Bateria desconectada = ignição desligada
            7 -> ioStatus == 1  // Movimento/Shock - usa status I/O
            else -> ioStatus == 1  // Fallback para status I/O (pacotes STT normais)
        }

        println("[${generateDate()}] 🔍 Debug GPS: type=$packetType, eventCode=$eventCode, ioStatus=$ioStatus, ignition=$ignition")

        // Parseia data/hora do dispositivo (campos 4 e 5)
        val dateStr = parts.getOrNull(4) // 20251101
        val timeStr = parts.getOrNull(5) // 13:10:37

        val timestamp = if (dateStr != null && timeStr != null) {
            parseDeviceDateTime(dateStr, timeStr)
        } else {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        GpsData(latitude, longitude, speed, heading, ignition, timestamp)
    } catch (e: Exception) {
        println("[${generateDate()}] Erro ao parsear GPS: ${e.message}")
        null
    }
}

// Função auxiliar para parsear data/hora do dispositivo
fun parseDeviceDateTime(dateStr: String, timeStr: String): LocalDateTime {
    return try {
        // dateStr = "20251101" -> ano=2025, mês=11, dia=01
        val year = dateStr.substring(0, 4).toInt()
        val month = dateStr.substring(4, 6).toInt()
        val day = dateStr.substring(6, 8).toInt()

        // timeStr = "13:10:37" -> hora=13, min=10, seg=37
        val timeParts = timeStr.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = timeParts[2].toInt()

        LocalDateTime(year, month, day, hour, minute, second)
    } catch (e: Exception) {
        println("[${generateDate()}] Erro ao parsear data/hora do dispositivo: ${e.message}")
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
}

suspend fun saveOrUpdateGps(imei: String, gps: GpsData, rawMessage: String = "") {
    val vehicleId = findVehicleIdByImei(imei)
    if (vehicleId == null) {
        println("⚠️ IMEI $imei não vinculado a nenhum veículo — ignorando pacote")
        return
    }

    DatabaseFactory.dbQuery {
        val existingDevice = GpsDevicesTable
            .selectAll()
            .where { GpsDevicesTable.imei eq imei }
            .singleOrNull()

        println(
            "[${generateDate()}] 🔹 Salvando GPS Suntech: " +
                    "vehicleId=$vehicleId, lat=${gps.latitude}, lon=${gps.longitude}, " +
                    "speed=${gps.speed}, heading=${gps.heading}, ign=${gps.ignition}, " +
                    "dateTime=${gps.dateTime}"
        )

        if (existingDevice == null) {
            println("⚠️ Dispositivo GPS com IMEI $imei não está cadastrado no sistema")
            return@dbQuery
        }

        val gpsDeviceId = existingDevice[GpsDevicesTable.id]

        // Atualiza posição do dispositivo
        GpsDevicesTable.update({ GpsDevicesTable.imei eq imei }) { row ->
            row[GpsDevicesTable.latitude] = gps.latitude.toBigDecimal()
            row[GpsDevicesTable.longitude] = gps.longitude.toBigDecimal()
            row[GpsDevicesTable.speed] = gps.speed.toBigDecimal()
            row[GpsDevicesTable.heading] = (gps.heading % 360.0).toBigDecimal()
            row[GpsDevicesTable.dateTime] = gps.dateTime
            row[GpsDevicesTable.ignition] = gps.ignition
        }

        // Salva no histórico
        GpsHistoryTable.insert { row ->
            row[GpsHistoryTable.gpsDeviceId] = gpsDeviceId
            row[GpsHistoryTable.vehicleId] = vehicleId
            row[GpsHistoryTable.dateTime] = gps.dateTime
            row[GpsHistoryTable.latitude] = gps.latitude.toBigDecimal()
            row[GpsHistoryTable.longitude] = gps.longitude.toBigDecimal()
            row[GpsHistoryTable.rawLog] = rawMessage
        }
        println("[${generateDate()}] ✅ Histórico GPS salvo - deviceId=$gpsDeviceId, vehicleId=$vehicleId")
    }
}

fun generateDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')} " +
            "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
}