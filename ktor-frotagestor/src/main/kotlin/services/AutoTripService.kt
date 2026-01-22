package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.*
import com.frotagestor.interfaces.TripStatus
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import kotlin.math.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class AutoTripService(
    private val httpClient: HttpClient = HttpClient()
) {

    companion object {
        // Timers de ignição
        private const val MIN_IGNITION_ON_SECONDS = 60
        private const val MIN_IGNITION_OFF_MINUTES = 10

        // Validações rigorosas de viagem
        private const val MIN_TRIP_DURATION_MINUTES = 3.0
        private const val MIN_TRIP_DISTANCE_KM = 0.3
        private const val MIN_MOVED_DISTANCE_KM = 0.15
        private const val MIN_VALID_TRIP_DISTANCE_KM = 1.5

        // Limpeza de viagens órfãs
        private const val PENDING_TRIP_CLEANUP_HOURS = 3

        // API de geocodificação
        private const val GEOCODING_API_URL = "https://nominatim.openstreetmap.org/reverse"
    }

    // Cache de ignição ligada (aguardando 60s)
    private val ignitionOnCache = mutableMapOf<Int, IgnitionOnEvent>()

    // Cache de ignição desligada (aguardando 10 min)
    private val ignitionOffCache = mutableMapOf<Int, IgnitionOffEvent>()

    data class IgnitionOnEvent(
        val vehicleId: Int,
        val latitude: Double,
        val longitude: Double,
        val dateTime: LocalDateTime,
        val imei: String
    )

    data class IgnitionOffEvent(
        val vehicleId: Int,
        val tripId: Int,
        val latitude: Double,
        val longitude: Double,
        val dateTime: LocalDateTime,
        val imei: String
    )

    /**
     * 🔥 Verifica timers pendentes e cria/finaliza viagens
     * Deve ser chamado em CADA pacote GPS recebido
     */
    suspend fun checkPendingTimers(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        currentTime: LocalDateTime,
        speed: Double
    ) {
        // Verifica timer de ignição ON (60s)
        checkPendingTripStart(vehicleId, latitude, longitude, currentTime, speed)

        // Verifica timer de ignição OFF (10 min)
        checkPendingTripEnd(vehicleId, latitude, longitude, currentTime)
    }

    /**
     * Verifica se o timer de 60s expirou e cria a viagem
     */
    private suspend fun checkPendingTripStart(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        currentTime: LocalDateTime,
        speed: Double
    ) {
        val pending = ignitionOnCache[vehicleId] ?: return

        val elapsedSeconds = calculateDuration(pending.dateTime, currentTime) * 60

        println("⏱️ Timer ignição ON: veículo=$vehicleId, decorrido=${elapsedSeconds.toInt()}s/${MIN_IGNITION_ON_SECONDS}s")

        if (elapsedSeconds >= MIN_IGNITION_ON_SECONDS) {
            createTripInDatabase(pending)
            ignitionOnCache.remove(vehicleId)
            println("✅ Timer ON expirou - viagem criada após ${elapsedSeconds.toInt()}s")
        }
    }

    /**
     * Verifica se o timer de 10 min expirou e finaliza a viagem
     */
    private suspend fun checkPendingTripEnd(
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        currentTime: LocalDateTime
    ) {
        val pending = ignitionOffCache[vehicleId] ?: return

        val elapsedMinutes = calculateDuration(pending.dateTime, currentTime)

        println("⏱️ Timer ignição OFF: veículo=$vehicleId, decorrido=${elapsedMinutes.toInt()}min/${MIN_IGNITION_OFF_MINUTES}min")

        if (elapsedMinutes >= MIN_IGNITION_OFF_MINUTES) {
            finalizeTripInDatabase(pending, latitude, longitude, currentTime)
            ignitionOffCache.remove(vehicleId)
            println("✅ Timer OFF expirou - finalizando viagem após ${elapsedMinutes.toInt()} min")
        }
    }

    /**
     * Detecta mudança de estado da ignição e gerencia timers
     */
    suspend fun processIgnitionChange(
        imei: String,
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        ignition: Boolean,
        dateTime: LocalDateTime,
        speed: Double
    ) {
        if (ignition) {
            handleIgnitionOn(imei, vehicleId, latitude, longitude, dateTime)
        } else {
            handleIgnitionOff(imei, vehicleId, latitude, longitude, dateTime)
        }

        cleanupPendingTrips()
    }

    /**
     * Atualiza viagem em andamento com novos dados GPS
     * Continua atualizando mesmo durante timer de 10 min
     */
    suspend fun updateActiveTrip(
        imei: String,
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        dateTime: LocalDateTime,
        speed: Double
    ) {
        DatabaseFactory.dbQuery {
            val activeTrip = TripsTable
                .selectAll()
                .where {
                    (TripsTable.vehicleId eq vehicleId) and
                            (TripsTable.status eq TripStatus.EM_ANDAMENTO) and
                            (TripsTable.autoGenerated eq true)
                }
                .orderBy(TripsTable.startTime, SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            if (activeTrip != null) {
                val tripId = activeTrip[TripsTable.id]
                val startLat = activeTrip[TripsTable.startLatitude]?.toDouble() ?: latitude
                val startLon = activeTrip[TripsTable.startLongitude]?.toDouble() ?: longitude
                val startTime = activeTrip[TripsTable.startTime]

                val distanceKm = calculateDistance(startLat, startLon, latitude, longitude)
                val durationMinutes = calculateDuration(startTime, dateTime)

                val currentMaxSpeed = activeTrip[TripsTable.maxSpeedKmh]?.toDouble() ?: 0.0
                val newMaxSpeed = maxOf(currentMaxSpeed, speed)

                TripsTable.update({ TripsTable.id eq tripId }) {
                    it[endLatitude] = latitude.toBigDecimal()
                    it[endLongitude] = longitude.toBigDecimal()
                    it[endTime] = dateTime
                    it[TripsTable.distanceKm] = distanceKm.toBigDecimal()
                    it[maxSpeedKmh] = newMaxSpeed.toBigDecimal()
                }

                println("🔄 Viagem #$tripId atualizada (${distanceKm.format(2)} km, ${durationMinutes.format(1)} min)")
            }
        }
    }

    /**
     * Registra ignição ligada e inicia timer de 60s
     */
    private suspend fun handleIgnitionOn(
        imei: String,
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        dateTime: LocalDateTime
    ) {
        // Se existe timer de finalização pendente → CANCELA e retoma viagem
        if (ignitionOffCache.containsKey(vehicleId)) {
            val offEvent = ignitionOffCache.remove(vehicleId)!!
            val elapsedMinutes = calculateDuration(offEvent.dateTime, dateTime)
            println("🔄 Ignição religada após ${elapsedMinutes.format(1)} min - timer OFF cancelado, viagem continua #${offEvent.tripId}")
            return
        }

        // Se já existe timer de ignição ON → mantém o original (não reseta)
        if (ignitionOnCache.containsKey(vehicleId)) {
            println("⏳ Timer de ignição ON já em andamento (veículo $vehicleId)")
            return
        }

        // Verifica se já existe viagem ativa
        val existingTrip = DatabaseFactory.dbQuery {
            TripsTable
                .selectAll()
                .where {
                    (TripsTable.vehicleId eq vehicleId) and
                            (TripsTable.status eq TripStatus.EM_ANDAMENTO) and
                            (TripsTable.autoGenerated eq true)
                }
                .singleOrNull()
        }

        if (existingTrip != null) {
            println("⚠️ Viagem já existe em andamento #${existingTrip[TripsTable.id]} (veículo $vehicleId)")
            return
        }

        // Cria novo timer de 60s
        ignitionOnCache[vehicleId] = IgnitionOnEvent(vehicleId, latitude, longitude, dateTime, imei)
        println("⏳ Ignição ligada - aguardando ${MIN_IGNITION_ON_SECONDS}s (veículo $vehicleId)")
    }

    /**
     * Cria viagem no banco de dados após validação de 60s
     */
    private suspend fun createTripInDatabase(event: IgnitionOnEvent) {
        DatabaseFactory.dbQuery {
            val gpsDeviceId = GpsDevicesTable
                .selectAll()
                .where { GpsDevicesTable.imei eq event.imei }
                .singleOrNull()
                ?.get(GpsDevicesTable.id)

            val defaultDriverId = VehiclesTable
                .selectAll()
                .where { VehiclesTable.id eq event.vehicleId }
                .singleOrNull()
                ?.get(VehiclesTable.defaultDriverId)

            val startAddress = getAddressFromCoordinates(event.latitude, event.longitude)

            val tripId = TripsTable.insert {
                it[vehicleId] = event.vehicleId
                it[driverId] = defaultDriverId
                it[TripsTable.gpsDeviceId] = gpsDeviceId
                it[startLatitude] = event.latitude.toBigDecimal()
                it[startLongitude] = event.longitude.toBigDecimal()
                it[startLocation] = startAddress ?: "Localização não disponível"
                it[startTime] = event.dateTime
                it[distanceKm] = 0.toBigDecimal()
                it[status] = TripStatus.EM_ANDAMENTO
                it[autoGenerated] = true
                it[maxSpeedKmh] = 0.toBigDecimal()
            }[TripsTable.id]

            println("🚗 Viagem criada #$tripId (veículo ${event.vehicleId})")
            println("   📍 Início: ${startAddress ?: "Não disponível"}")
        }
    }

    /**
     * Registra ignição desligada e inicia timer de 10 min
     */
    private suspend fun handleIgnitionOff(
        imei: String,
        vehicleId: Int,
        latitude: Double,
        longitude: Double,
        dateTime: LocalDateTime
    ) {
        // Se existe timer de ignição ON pendente → CANCELA e descarta
        if (ignitionOnCache.containsKey(vehicleId)) {
            val pending = ignitionOnCache.remove(vehicleId)!!
            val elapsedSeconds = calculateDuration(pending.dateTime, dateTime) * 60
            println("🚫 Ignição descartada - ligou/desligou em ${elapsedSeconds.toInt()}s (< ${MIN_IGNITION_ON_SECONDS}s)")
            return
        }

        // Se já existe timer de finalização → mantém o original
        if (ignitionOffCache.containsKey(vehicleId)) {
            println("⏳ Timer de finalização já em andamento (veículo $vehicleId)")
            return
        }

        // 🔥 CORREÇÃO: Busca viagem ativa E extrai tripId DENTRO do dbQuery
        val tripId = DatabaseFactory.dbQuery {
            val activeTrip = TripsTable
                .selectAll()
                .where {
                    (TripsTable.vehicleId eq vehicleId) and
                            (TripsTable.status eq TripStatus.EM_ANDAMENTO) and
                            (TripsTable.autoGenerated eq true)
                }
                .singleOrNull()

            // Retorna o ID da viagem (ou null)
            activeTrip?.get(TripsTable.id)
        }

        if (tripId == null) {
            println("⚠️ Nenhuma viagem ativa para iniciar timer de finalização (veículo $vehicleId)")
            return
        }

        // Cria timer de 10 minutos
        ignitionOffCache[vehicleId] = IgnitionOffEvent(vehicleId, tripId, latitude, longitude, dateTime, imei)
        println("⏳ Ignição desligada - aguardando ${MIN_IGNITION_OFF_MINUTES} min para finalizar viagem #$tripId")
    }

    /**
     * Finaliza viagem após timer de 10 min com validações rigorosas
     */
    private suspend fun finalizeTripInDatabase(
        event: IgnitionOffEvent,
        currentLatitude: Double,
        currentLongitude: Double,
        currentTime: LocalDateTime
    ) {
        DatabaseFactory.dbQuery {
            val activeTrip = TripsTable
                .selectAll()
                .where { TripsTable.id eq event.tripId }
                .singleOrNull()

            if (activeTrip == null) {
                println("⚠️ Viagem #${event.tripId} não encontrada para finalização")
                return@dbQuery
            }

            val tripId = activeTrip[TripsTable.id]
            val startLat = activeTrip[TripsTable.startLatitude]?.toDouble() ?: currentLatitude
            val startLon = activeTrip[TripsTable.startLongitude]?.toDouble() ?: currentLongitude
            val startTime = activeTrip[TripsTable.startTime]

            // Usa coordenadas mais recentes do banco (atualizadas durante os 10 min)
            val endLat = activeTrip[TripsTable.endLatitude]?.toDouble() ?: currentLatitude
            val endLon = activeTrip[TripsTable.endLongitude]?.toDouble() ?: currentLongitude

            // Calcula métricas finais
            val totalDistanceKm = calculateDistance(startLat, startLon, endLat, endLon)
            val durationMinutes = calculateDuration(startTime, currentTime)

            // 🔥 VALIDAÇÃO RIGOROSA FINAL
            if (totalDistanceKm < MIN_VALID_TRIP_DISTANCE_KM) {
                TripsTable.deleteWhere { TripsTable.id eq tripId }
                println("🗑️ Viagem #$tripId DELETADA - distância insuficiente")
                println("   📏 Distância: ${totalDistanceKm.format(3)} km (< ${MIN_VALID_TRIP_DISTANCE_KM} km)")
                println("   ⏱️  Duração: ${durationMinutes.format(1)} min")
                return@dbQuery
            }

            // Viagem válida → finaliza e salva
            val endAddress = getAddressFromCoordinates(endLat, endLon)
            val maxSpeed = activeTrip[TripsTable.maxSpeedKmh]?.toDouble() ?: 0.0
            val avgSpeed = if (durationMinutes > 0) {
                (totalDistanceKm / durationMinutes) * 60
            } else 0.0

            TripsTable.update({ TripsTable.id eq tripId }) {
                it[TripsTable.endLatitude] = endLat.toBigDecimal()
                it[TripsTable.endLongitude] = endLon.toBigDecimal()
                it[TripsTable.endLocation] = endAddress ?: "Localização não disponível"
                it[TripsTable.endTime] = currentTime
                it[TripsTable.distanceKm] = totalDistanceKm.toBigDecimal()
                it[TripsTable.status] = TripStatus.CONCLUIDA
                it[TripsTable.maxSpeedKmh] = maxSpeed.toBigDecimal()
            }

            println("✅ Viagem #$tripId FINALIZADA E SALVA")
            println("   📏 Distância: ${totalDistanceKm.format(2)} km")
            println("   ⏱️  Duração: ${durationMinutes.toInt()} min")
            println("   🚀 Vel. máx: ${maxSpeed.format(1)} km/h | Vel. média: ${avgSpeed.format(1)} km/h")
            println("   📍 Fim: ${endAddress ?: "Não disponível"}")
        }
    }

    /**
     * Limpa viagens órfãs (EM_ANDAMENTO há muito tempo)
     */
    private suspend fun cleanupPendingTrips() {
        DatabaseFactory.dbQuery {
            val cutoffTime = Clock.System.now()
                .minus(PENDING_TRIP_CLEANUP_HOURS, DateTimeUnit.HOUR, TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val orphanTrips = TripsTable
                .selectAll()
                .where {
                    (TripsTable.status eq TripStatus.EM_ANDAMENTO) and
                            (TripsTable.startTime less cutoffTime) and
                            (TripsTable.autoGenerated eq true)
                }
                .toList()

            orphanTrips.forEach { trip ->
                val tripId = trip[TripsTable.id]
                val startLat = trip[TripsTable.startLatitude]?.toDouble() ?: 0.0
                val startLon = trip[TripsTable.startLongitude]?.toDouble() ?: 0.0
                val endLat = trip[TripsTable.endLatitude]?.toDouble() ?: startLat
                val endLon = trip[TripsTable.endLongitude]?.toDouble() ?: startLon
                val distance = calculateDistance(startLat, startLon, endLat, endLon)

                if (distance < MIN_MOVED_DISTANCE_KM) {
                    TripsTable.deleteWhere { TripsTable.id eq tripId }
                    println("🧹 Viagem órfã #$tripId descartada (distância: ${distance.format(3)} km)")
                } else {
                    TripsTable.update({ TripsTable.id eq tripId }) {
                        it[TripsTable.status] = TripStatus.CONCLUIDA
                        it[TripsTable.distanceKm] = distance.toBigDecimal()
                    }
                    println("🧹 Viagem órfã #$tripId finalizada automaticamente (${distance.format(2)} km)")
                }
            }
        }
    }

    /**
     * Geocoding Reverso: Converte coordenadas em endereço
     */
    private suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            val response: HttpResponse = httpClient.get(GEOCODING_API_URL) {
                parameter("lat", latitude)
                parameter("lon", longitude)
                parameter("format", "json")
                parameter("addressdetails", 1)
                header("User-Agent", "FrotaGestor/1.0")
            }

            val jsonResponse = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val address = jsonResponse["address"]?.jsonObject

            val road = address?.get("road")?.jsonPrimitive?.content
            val suburb = address?.get("suburb")?.jsonPrimitive?.content
            val city = address?.get("city")?.jsonPrimitive?.content
                ?: address?.get("town")?.jsonPrimitive?.content
                ?: address?.get("village")?.jsonPrimitive?.content
            val state = address?.get("state")?.jsonPrimitive?.content

            buildString {
                road?.let { append(it) }
                if (suburb != null && suburb != road) {
                    if (isNotEmpty()) append(", ")
                    append(suburb)
                }
                city?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                state?.let {
                    if (isNotEmpty()) append(" - ")
                    append(it)
                }
            }.takeIf { it.isNotEmpty() }

        } catch (e: Exception) {
            println("⚠️ Erro ao buscar endereço: ${e.message}")
            null
        }
    }

    /**
     * Calcula distância entre dois pontos usando fórmula de Haversine
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * Calcula duração entre dois momentos em minutos
     */
    private fun calculateDuration(start: LocalDateTime, end: LocalDateTime): Double {
        val startInstant = start.toInstant(TimeZone.currentSystemDefault())
        val endInstant = end.toInstant(TimeZone.currentSystemDefault())
        val durationMs = (endInstant - startInstant).inWholeMilliseconds
        return durationMs / 60000.0
    }

    /**
     * Formata número com casas decimais específicas
     */
    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}