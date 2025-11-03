package com.frotagestor.protocols_devices_gps.suntech

/**
 * Builder para comandos do Suntech ST-310
 * Documentação: Suntech ST-310U Protocol SA200
 */
object SuntechCommandBuilder {
    /**
     * Configura parâmetros de rede (APN, servidor, porta)
     * SA200NTW;DEV_ID;02;AUTH;APN;USER_ID;USER_PWD;SERVER_IP;SERVER_PORT;BACKUP_IP;BACKUP_PORT;SMS_NO;PIN_NO
     */
    fun buildNetworkConfigCommand(
        deviceId: String,
        apn: String,
        serverIp: String,
        serverPort: Int,
        userId: String = "",
        userPwd: String = "",
        backupIp: String = "",
        backupPort: Int = 0
    ): ByteArray {
        val backupPortStr = if (backupPort > 0) backupPort.toString() else ""

        val command = "SA200NTW;$deviceId;02;0;$apn;$userId;$userPwd;$serverIp;$serverPort;$backupIp;$backupPortStr;;\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Configura intervalos de relatório
     * SA200RPT;DEV_ID;02;T1;T2;T3;A1;SND_DIST;T4;SMS_T1;SMS_T2;SMS_PACK_NO;ANGLE_RPT;RPT_TYPE
     *
     * @param parkingInterval Intervalo em modo estacionado (segundos) - padrão 180s (3min)
     * @param drivingInterval Intervalo em modo dirigindo (segundos) - padrão 30s
     * @param emergencyInterval Intervalo em modo emergência (segundos) - padrão 10s
     * @param keepAliveInterval Intervalo de heartbeat (minutos) - padrão 3min
     */
    fun buildReportIntervalCommand(
        deviceId: String,
        parkingInterval: Int = 180,
        drivingInterval: Int = 30,
        emergencyInterval: Int = 10,
        keepAliveInterval: Int = 3
    ): ByteArray {
        val command = "SA200RPT;$deviceId;02;$parkingInterval;$drivingInterval;$emergencyInterval;3;0;$keepAliveInterval;0;0;0;0;0\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Configura parâmetros de eventos (ignição, entradas digitais)
     * SA200EVT;DEV_ID;02;IGNITION;T1;T2;IN1_TYPE;IN2_TYPE;IN3_TYPE;...
     *
     * @param ignitionMode 0=Desabilitado, 1=Por tensão (12V/24V), 2=Virtual Ignition
     * @param in1Type Tipo da entrada 1: 0=Falling Edge, 1=Rising Edge, 2=Both Edge, 3=Panic Button
     */
    fun buildEventConfigCommand(
        deviceId: String,
        ignitionMode: Int = 1,
        in1Type: Int = 3,  // 3 = Detecção de tensão (padrão para ignição)
        in2Type: Int = 8,  // 8 = No Use
        in3Type: Int = 8   // 8 = No Use
    ): ByteArray {
        val command = "SA200EVT;$deviceId;02;$ignitionMode;60;0;$in1Type;$in2Type;$in3Type;30;0;0;1;0;1;0;0;0;0;0;0;0;0;0;0;0;0\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Configura parâmetros de serviço (verificações de sistema)
     * SA200SVC;DEV_ID;02;PARKING_LOCK;SPEED_LIMIT;PWR_DN;...
     *
     * @param mainPowerCheck Verifica desconexão da alimentação principal
     * @param antennaCheck Verifica desconexão da antena GPS
     * @param batteryCheck Verifica erro na bateria backup
     */
    fun buildServiceConfigCommand(
        deviceId: String,
        mainPowerCheck: Boolean = true,
        antennaCheck: Boolean = true,
        batteryCheck: Boolean = true,
        speedLimit: Int = 0  // 0 = desabilitado, ou velocidade em km/h
    ): ByteArray {
        val mpChk = if (mainPowerCheck) "1" else "0"
        val antChk = if (antennaCheck) "1" else "0"
        val batChk = if (batteryCheck) "1" else "0"

        val command = "SA200SVC;$deviceId;02;0;$speedLimit;0;0;0;0;$mpChk;$antChk;$batChk;0;0;0;0;0;0;0;0\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Solicita posição atual (força envio de relatório STT)
     */
    fun buildRequestPositionCommand(deviceId: String): ByteArray {
        val command = "SA200CMD;$deviceId;02;StatusReq\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Reset do dispositivo (reinicia com configurações atuais)
     */
    fun buildResetCommand(deviceId: String): ByteArray {
        val command = "SA200CMD;$deviceId;02;Reset\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Solicita configurações atuais do dispositivo
     */
    fun buildPresetCommand(deviceId: String): ByteArray {
        val command = "SA200CMD;$deviceId;02;Preset\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Controla saída 1 (imobilizador/relé)
     * @param activate true para ativar, false para desativar
     */
    fun buildOutput1Command(deviceId: String, activate: Boolean): ByteArray {
        val state = if (activate) "1" else "0"
        val command = "SA200CMD;$deviceId;02;Output1;$state\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }

    /**
     * Configura cerca geográfica (geofence)
     * @param enabled true para habilitar, false para desabilitar
     */
    fun buildGeofenceCommand(
        deviceId: String,
        enabled: Boolean,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        radius: Int = 1000  // metros
    ): ByteArray {
        val state = if (enabled) "1" else "0"
        val command = "SA200CMD;$deviceId;02;GeoFence;$state;$latitude;$longitude;$radius\r\n"
        return command.toByteArray(Charsets.US_ASCII)
    }
}