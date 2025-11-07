package com.frotagestor.protocols_devices_gps.suntech

import com.frotagestor.accurate_gt_06.findVehicleIdByImei
import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.database.models.GpsHistoryTable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
    onAck: suspend (String, String) -> Unit  // (imei, comando)
) {
    when {
        msg.startsWith("ST300ALV;") -> {
            val id = msg.substringAfter("ST300ALV;").substringBefore(";").trim()
            println("[${generateDate()}] HEARTBEAT (ALV) – Device ID: $id")
        }

        msg.startsWith("ST300GPS;") || msg.startsWith("ST300STT;") -> {
            val id = if (currentId == null) {
                extractDeviceId(msg) ?: return
            } else {
                currentId
            }

            val gps = parseGpsPacket(msg) ?: return
            saveOrUpdateGps(id, gps, msg)
            println("[${generateDate()}] Posição recebida – ID: $id, Lat: ${gps.latitude}, Lon: ${gps.longitude}")
        }

        msg.startsWith("ST300CMD;Res;") -> {
            val id = extractDeviceId(msg) ?: currentId ?: "UNKNOWN"
            println("[${generateDate()}] Resposta de comando recebida: $msg")
            val parts = msg.split(";")
            val commandType = parts.getOrNull(3)
            val result = parts.getOrNull(4)
            println("[${generateDate()}] Comando: $commandType, Resultado: $result")
            val gps = parseGpsPacket(msg)
            if (gps != null && id != "UNKNOWN") {
                saveOrUpdateGps(id, gps, msg)
                println("[${generateDate()}] Posição atualizada via CMD – ID: $id")
            }
        }

        msg.startsWith("ST300ALT;") -> {
            val id = extractDeviceId(msg) ?: currentId ?: return
            val gps = parseGpsPacket(msg) ?: return
            saveOrUpdateGps(id, gps, msg)
            val parts = msg.split(";")
            val alertId = parts.getOrNull(3)?.toIntOrNull()
            val eventDescription = when (alertId) {
                1 -> "Ignição LIGADA"
                2 -> "Ignição DESLIGADA"
                3 -> "Entrada 1 ativa"
                4 -> "Entrada 1 inativa"
                5 -> "Entrada 2 ativa"
                6 -> "Entrada 2 inativa"
                18 -> "Excesso de velocidade"
                19 -> "Bateria principal desconectada"
                23 -> "Movimento no estacionamento"
                else -> "Alerta ID: $alertId"
            }
            println("[${generateDate()}] ALERTA (ALT) – ID: $id – $eventDescription")
        }

        msg.startsWith("ST300EMG;") -> {
            val id = extractDeviceId(msg) ?: currentId ?: return
            val gps = parseGpsPacket(msg) ?: return
            saveOrUpdateGps(id, gps, msg)

            val parts = msg.split(";")
            val emergencyMode = parts.getOrNull(3)?.toIntOrNull()
            val emergencyType = when (emergencyMode) {
                1 -> "Botão de pânico acionado"
                2 -> "Movimento sem ignição"
                3 -> "Bateria principal desconectada"
                else -> "Emergência modo: $emergencyMode"
            }
            println("[${generateDate()}] EMERGÊNCIA (EMG) – ID: $id – $emergencyType")
        }

        msg.startsWith("ST300EVT;") -> {
            val id = extractDeviceId(msg) ?: currentId ?: return
            val gps = parseGpsPacket(msg) ?: return
            saveOrUpdateGps(id, gps, msg)

            val parts = msg.split(";")
            val eventType = parts.getOrNull(3)?.toIntOrNull()
            println("[${generateDate()}] EVENTO (EVT) – ID: $id – Tipo: $eventType")
        }

        else -> {
            println("[${generateDate()}] ⚠️ Pacote desconhecido: $msg")
            // Não envia NAK, apenas loga
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
    return try {
        when {
            message.startsWith("ST300ALV;") -> message.substringAfter("ST300ALV;").substringBefore(";").trim()
            message.startsWith("ST300GPS;") -> message.substringAfter("ST300GPS;").substringBefore(";").trim()
            message.startsWith("ST300STT;") -> message.substringAfter("ST300STT;").substringBefore(";").trim()
            message.startsWith("ST300ALT;") -> message.substringAfter("ST300ALT;").substringBefore(";").trim()
            message.startsWith("ST300EMG;") -> message.substringAfter("ST300EMG;").substringBefore(";").trim()
            message.startsWith("ST300EVT;") -> message.substringAfter("ST300EVT;").substringBefore(";").trim()
            message.startsWith("ST300CMD;Res;") -> {
                message.split(";").getOrNull(2)?.trim()
            }
            else -> null
        }
    } catch (e: Exception) {
        println("[${generateDate()}] Erro ao extrair Device ID: ${e.message}")
        null
    }
}

fun parseGpsPacket(data: String): GpsData? {
    return try {
        val parts = data.split(";")
        val packetType = parts.getOrNull(0) ?: ""
        val latitude = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0
        val longitude = parts.getOrNull(8)?.toDoubleOrNull() ?: 0.0
        val speed = parts.getOrNull(9)?.toDoubleOrNull() ?: 0.0
        val heading = parts.getOrNull(10)?.toDoubleOrNull() ?: 0.0
        val eventCode = if (packetType.contains("ALT") || packetType.contains("EMG")) {
            parts.getOrNull(16)?.toIntOrNull()
        } else { null }
        val ioStatus = parts.lastOrNull()?.toIntOrNull()
        val ignition = when (eventCode) {
            40 -> true   // Evento de ignição ligada
            41 -> false  // Evento de ignição desligada
            3 -> false   // Bateria desconectada = ignição desligada
            7 -> ioStatus == 1  // Movimento/Shock - usa status I/O
            else -> ioStatus == 1  // Fallback para status I/O (pacotes STT normais)
        }

        println("[${generateDate()}] 🔍 Debug GPS: type=$packetType, eventCode=$eventCode, ioStatus=$ioStatus, ignition=$ignition")
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

fun parseDeviceDateTime(dateStr: String, timeStr: String): LocalDateTime {
    return try {
        val year = dateStr.substring(0, 4).toInt()
        val month = dateStr.substring(4, 6).toInt()
        val day = dateStr.substring(6, 8).toInt()
        val timeParts = timeStr.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = timeParts[2].toInt()
        val utcDateTime = LocalDateTime(year, month, day, hour, minute, second)
        val localTz = TimeZone.currentSystemDefault()
        val instant = utcDateTime.toInstant(TimeZone.UTC)
        instant.toLocalDateTime(localTz)
    } catch (e: Exception) {
        println("[${generateDate()}] ❌ Erro ao parsear data/hora: $dateStr $timeStr - ${e.message}")
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
            row[GpsDevicesTable.speed] = gps.speed.toBigDecimal()
            row[GpsHistoryTable.latitude] = gps.latitude.toBigDecimal()
            row[GpsHistoryTable.longitude] = gps.longitude.toBigDecimal()
            row[GpsHistoryTable.rawLog] = rawMessage
        }
        println("[${generateDate()}] ✅ GPS salvo - vehicleId=$vehicleId, lat=${gps.latitude}, lon=${gps.longitude}, ign=${gps.ignition}")
    }
}

fun generateDate(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')} " +
            "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
}