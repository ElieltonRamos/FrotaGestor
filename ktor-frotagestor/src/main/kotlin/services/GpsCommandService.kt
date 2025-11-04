package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.GpsDevicesTable
import com.frotagestor.interfaces.CommandRequest
import com.frotagestor.interfaces.CommandResponse
import com.frotagestor.interfaces.ConnectedDeviceDto
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.protocols_devices_gps.suntech.DeviceConnectionManager
import com.frotagestor.protocols_devices_gps.suntech.extractDeviceId
import io.ktor.http.HttpStatusCode
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.writeFully
import org.jetbrains.exposed.sql.selectAll

class GpsCommandService {

    suspend fun sendCommand(
        deviceId: String,
        request: CommandRequest
    ): ServiceResponse<CommandResponse> {

        // 1. Valida no banco
        val device = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll()
                .where { GpsDevicesTable.imei eq deviceId }
                .singleOrNull()
        } ?: return ServiceResponse(
            HttpStatusCode.NotFound,
            CommandResponse(false, "Dispositivo GPS não encontrado com DeviceId: $deviceId")
        )

        // 2. Verifica conexão
        if (!DeviceConnectionManager.isDeviceConnected(deviceId)) {
            return ServiceResponse(
                HttpStatusCode.ServiceUnavailable,
                CommandResponse(false, "Dispositivo não está conectado ao servidor TCP")
            )
        }

        // 3. Constrói comando
        val command = buildSuntechCommand(deviceId, request)

        // 4. ENVIA USANDO O MANAGER (ÚNICO PONTO DE ENVIO!)
        val success = DeviceConnectionManager.sendCommand(deviceId, command)

        return if (success) {
            ServiceResponse(HttpStatusCode.OK, CommandResponse(true, "Comando enviado com sucesso", command))
        } else {
            ServiceResponse(HttpStatusCode.ServiceUnavailable, CommandResponse(false, "Falha ao enviar comando"))
        }
    }

    /**
     * Solicita localização imediata do dispositivo
     */
    suspend fun requestLocation(deviceId: String): ServiceResponse<CommandResponse> {
        return sendCommand(
            deviceId,
            CommandRequest(commandType = "CMD", parameters = mapOf("action" to "location"))
        )
    }

    /**
     * Bloqueia/Desbloqueia o motor do veículo
     */
    suspend fun controlImmobilizer(
        deviceId: String,
        enable: Boolean
    ): ServiceResponse<CommandResponse> {
        return sendCommand(
            deviceId,
            CommandRequest(
                commandType = "OUT",
                parameters = mapOf(
                    "output" to "1",
                    "status" to if (enable) "1" else "0"
                )
            )
        )
    }

    /**
     * Define os intervalos de envio de relatórios
     */
    suspend fun setReportIntervals(
        deviceId: String,
        drivingInterval: Int,
        parkingInterval: Int
    ): ServiceResponse<CommandResponse> {
        return sendCommand(
            deviceId,
            CommandRequest(
                commandType = "RPT",
                parameters = mapOf(
                    "driving" to drivingInterval.toString(),
                    "parking" to parkingInterval.toString()
                )
            )
        )
    }

    /**
     * Reinicia o dispositivo GPS
     */
    suspend fun rebootDevice(deviceId: String): ServiceResponse<CommandResponse> {
        return sendCommand(
            deviceId,
            CommandRequest(commandType = "RST")
        )
    }

    /**
     * Configura o servidor TCP/UDP
     */
    suspend fun configureServer(
        deviceId: String,
        serverIp: String,
        serverPort: Int,
        protocolType: String = "T" // T=TCP, U=UDP
    ): ServiceResponse<CommandResponse> {
        return sendCommand(
            deviceId,
            CommandRequest(
                commandType = "ADP",
                parameters = mapOf(
                    "serverType" to protocolType,
                    "ip" to serverIp,
                    "port" to serverPort.toString()
                )
            )
        )
    }

    /**
     * Obtém status dos dispositivos conectados
     */
    suspend fun getConnectedDevices(): ServiceResponse<List<ConnectedDeviceDto>> {
        val devices = DatabaseFactory.dbQuery {
            GpsDevicesTable.selectAll().map { row ->
                val imei = row[GpsDevicesTable.imei]
                ConnectedDeviceDto(
                    id = row[GpsDevicesTable.id],
                    imei = imei,
                    vehicleId = row[GpsDevicesTable.vehicleId],
                    title = row[GpsDevicesTable.title],
                    connected = DeviceConnectionManager.isDeviceConnected(imei)
                )
            }
        }
        return ServiceResponse(HttpStatusCode.OK, devices)
    }

    /**
     * Constrói o comando no formato do protocolo Suntech ST-310
     * Formato: ST300[CMD_TYPE];[DEV_ID];02;[PARAMS]\r
     */
    private fun buildSuntechCommand(deviceId: String, request: CommandRequest): String {
        return when (request.commandType) {
            "CMD" -> {
                // Comando para solicitar localização
                "ST300CMD;$deviceId;02"
            }
            "OUT" -> {
                // Controle de saídas (imobilizador)
                val output = request.parameters["output"] ?: "1"
                val status = request.parameters["status"] ?: "0"
                "ST300OUT;$deviceId;02;$output;$status"
            }
            "RPT" -> {
                // Configuração de intervalos de relatórios
                val driving = request.parameters["driving"] ?: "60"
                val parking = request.parameters["parking"] ?: "300"
                "ST300RPT;$deviceId;02;$driving;$parking;0"
            }
            "RST" -> {
                // Reset do dispositivo
                "ST300RST;$deviceId;02"
            }
            "ADP" -> {
                // Configuração de servidor
                val serverType = request.parameters["serverType"] ?: "T"
                val ip = request.parameters["ip"] ?: ""
                val port = request.parameters["port"] ?: "3002"
                "ST300ADP;$deviceId;02;$serverType;$ip;$port"
            }
            "SVC" -> {
                // Configuração de serviços (speed limit, parking lock, etc)
                val params = request.parameters.values.joinToString(";")
                "ST300SVC;$deviceId;02;$params"
            }
            "EVT" -> {
                // Configuração de eventos
                val params = request.parameters.values.joinToString(";")
                "ST300EVT;$deviceId;02;$params"
            }
            else -> {
                // Comando customizado
                val params = request.parameters.values.joinToString(";")
                "ST300${request.commandType};$deviceId;02${if (params.isNotEmpty()) ";$params" else ""}"
            }
        }
    }
}