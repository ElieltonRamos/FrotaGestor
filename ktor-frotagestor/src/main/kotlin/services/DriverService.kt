package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.DriversTable
import com.frotagestor.interfaces.Driver
import com.frotagestor.interfaces.DriverIndicators
import com.frotagestor.interfaces.DriverStatus
import com.frotagestor.interfaces.Expense
import com.frotagestor.interfaces.Message
import com.frotagestor.interfaces.PaginatedResponse
import com.frotagestor.interfaces.ServiceResponse
import com.frotagestor.interfaces.Vehicle
import com.frotagestor.interfaces.VehicleStatus
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateDriver
import com.frotagestor.validations.validatePartialDriver
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import kotlin.let
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class DriverService {

    suspend fun createDriver(req: String): ServiceResponse<Message> {
        val newDriver = validateDriver(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingDriver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.cpf eq newDriver.cpf }
                .singleOrNull()
        }

        if (existingDriver != null) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Motorista já registrado!")
            )
        }

        DatabaseFactory.dbQuery {
            DriversTable.insert {
                it[name] = newDriver.name
                it[cpf] = newDriver.cpf
                it[cnh] = newDriver.cnh
                it[cnhCategory] = newDriver.cnhCategory
                it[cnhExpiration] = newDriver.cnhExpiration
                it[phone] = newDriver.phone
                it[email] = newDriver.email
                it[status] = newDriver.status
                it[deletedAt] = null
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Motorista criado com sucesso")
        )
    }

    suspend fun updateDriver(id: Int, req: String): ServiceResponse<Message> {
        val updatedDriver = validatePartialDriver(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingDriver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.id eq id and DriversTable.deletedAt.isNull() }
                .singleOrNull()
        }

        if (existingDriver == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Motorista não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            DriversTable.update({ DriversTable.id eq id }) {
                updatedDriver.name?.let { n -> it[name] = n }
                updatedDriver.cpf?.let { c -> it[cpf] = c }
                updatedDriver.cnh?.let { c -> it[cnh] = c }
                updatedDriver.cnhCategory?.let { cat -> it[cnhCategory] = cat }
                updatedDriver.cnhExpiration?.let { exp -> it[cnhExpiration] = exp }
                updatedDriver.phone?.let { p -> it[phone] = p }
                updatedDriver.email?.let { e -> it[email] = e }
                updatedDriver.status?.let { s -> it[status] = s }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Motorista atualizado com sucesso")
        )
    }

    suspend fun getAllDrivers(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = DriversTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        nameFilter: String? = null,
        cpfFilter: String? = null,
        cnhFilter: String? = null,
        cnhCategoryFilter: String? = null,
        cnhExpirationFilter: LocalDate? = null,
        phoneFilter: String? = null,
        emailFilter: String? = null,
        statusFilter: DriverStatus? = null
    ): ServiceResponse<PaginatedResponse<Driver>> {
        return DatabaseFactory.dbQuery {
            val query = DriversTable
                .selectAll()
                .apply {
                    if (statusFilter != DriverStatus.INATIVO) {
                        andWhere { DriversTable.deletedAt.isNull() }
                    }
                    if (idFilter != null) {
                        andWhere { DriversTable.id eq idFilter }
                    }
                    if (!nameFilter.isNullOrBlank()) {
                        andWhere { DriversTable.name like "%$nameFilter%" }
                    }
                    if (!cpfFilter.isNullOrBlank()) {
                        andWhere { DriversTable.cpf like "%$cpfFilter%" }
                    }
                    if (!cnhFilter.isNullOrBlank()) {
                        andWhere { DriversTable.cnh like "%$cnhFilter%" }
                    }
                    if (!cnhCategoryFilter.isNullOrBlank()) {
                        andWhere { DriversTable.cnhCategory eq cnhCategoryFilter }
                    }
                    if (cnhExpirationFilter != null) {
                        andWhere { DriversTable.cnhExpiration eq cnhExpirationFilter }
                    }
                    if (!phoneFilter.isNullOrBlank()) {
                        andWhere { DriversTable.phone like "%$phoneFilter%" }
                    }
                    if (!emailFilter.isNullOrBlank()) {
                        andWhere { DriversTable.email like "%$emailFilter%" }
                    }
                    if (statusFilter != null) {
                        andWhere { DriversTable.status eq statusFilter }
                    }
                }

            val total = query.count()

            val orderExpr = if (sortBy == DriversTable.cnhCategory) {
                CustomFunction<String>("UPPER", TextColumnType(), DriversTable.cnhCategory)
            } else {
                sortBy
            }

            val results = query
                .orderBy(orderExpr to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Driver(
                        id = it[DriversTable.id],
                        name = it[DriversTable.name],
                        cpf = it[DriversTable.cpf],
                        cnh = it[DriversTable.cnh],
                        cnhCategory = it[DriversTable.cnhCategory],
                        cnhExpiration = it[DriversTable.cnhExpiration],
                        phone = it[DriversTable.phone],
                        email = it[DriversTable.email],
                        status = it[DriversTable.status],
                        deletedAt = it[DriversTable.deletedAt]
                    )
                }


            ServiceResponse(
                status = HttpStatusCode.OK,
                data = PaginatedResponse(
                    data = results,
                    total = total.toInt(),
                    page = page,
                    limit = limit,
                    totalPages = if (total == 0L) 0 else ((total + limit - 1) / limit).toInt()
                )
            )
        }
    }

    suspend fun findDriverById(id: Int): ServiceResponse<Any> {
        val driver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.id eq id and DriversTable.deletedAt.isNull() }
                .singleOrNull()
                ?.let {
                    Driver(
                        id = it[DriversTable.id],
                        name = it[DriversTable.name],
                        cpf = it[DriversTable.cpf],
                        cnh = it[DriversTable.cnh],
                        cnhCategory = it[DriversTable.cnhCategory],
                        cnhExpiration = it[DriversTable.cnhExpiration],
                        phone = it[DriversTable.phone],
                        email = it[DriversTable.email],
                        status = it[DriversTable.status]
                    )
                }
        }

        return if (driver == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = mapOf("message" to "Motorista não encontrado")
            )
        } else {
            ServiceResponse(
                status = HttpStatusCode.OK,
                data = driver
            )
        }
    }

    suspend fun softDeleteDriver(id: Int): ServiceResponse<Message> {
        val existingDriver = DatabaseFactory.dbQuery {
            DriversTable
                .selectAll()
                .where { DriversTable.id eq id and DriversTable.deletedAt.isNull() }
                .singleOrNull()
        }

        if (existingDriver == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Motorista não encontrado!")
            )
        }

        DatabaseFactory.dbQuery {
            DriversTable.update({ DriversTable.id eq id }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Motorista removido com sucesso")
        )
    }

    suspend fun getDriversIndicators(): ServiceResponse<DriverIndicators> = DatabaseFactory.dbQuery {
        // Define a data atual e a data limite para CNHs vencendo em 30 dias
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val thirtyDaysFromNow = now.plus(DatePeriod(days = 30))

        val sql = """
        SELECT
            COUNT(*) AS total_drivers,
            SUM(CASE WHEN cnh_expiration < '$now' THEN 1 ELSE 0 END) AS expired_licenses,
            SUM(CASE WHEN cnh_expiration BETWEEN '$now' AND '$thirtyDaysFromNow' THEN 1 ELSE 0 END) AS expiring_licenses,
            (SELECT cnh_category
             FROM drivers
             WHERE cnh_category IS NOT NULL
             GROUP BY cnh_category
             ORDER BY COUNT(*) DESC LIMIT 1) AS most_common_category,
            (SELECT name
             FROM drivers
             ORDER BY id DESC LIMIT 1) AS last_driver_name,
            (SELECT cpf
             FROM drivers
             ORDER BY id DESC LIMIT 1) AS last_driver_cpf
        FROM drivers
        WHERE deleted_at IS NULL AND status = 'ATIVO'
    """.trimIndent()

        var indicators: DriverIndicators? = null

        transaction {
            exec(sql) { rs ->
                if (rs.next()) {
                    indicators = DriverIndicators(
                        total = rs.getInt("total_drivers"),
                        withExpiredLicense = rs.getInt("expired_licenses"),
                        withExpiringLicense = rs.getInt("expiring_licenses"),
                        mostCommonCategory = rs.getString("most_common_category"),
                        lastDriver = DriverIndicators.LastDriver(
                            name = rs.getString("last_driver_name") ?: "Desconhecido",
                            cpf = rs.getString("last_driver_cpf") ?: "Desconhecido",
                        )
                    )
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = indicators ?: DriverIndicators(
                total = 0,
                withExpiredLicense = 0,
                withExpiringLicense = 0,
                mostCommonCategory = null,
                lastDriver = DriverIndicators.LastDriver(
                    name = "Desconhecido",
                    cpf = "Desconhecido",
                )
            )
        )
    }

    suspend fun getVehiclesByDriver(
        driverId: Int,
        page: Int = 1,
        limit: Int = 5,
        sortBy: String = "id",
        sortOrder: SortOrder = SortOrder.ASC,
        filters: Map<String, Any> = emptyMap()
    ): ServiceResponse<PaginatedResponse<Vehicle>> = DatabaseFactory.dbQuery {
        // Monta a query SQL
        val sortColumn = when (sortBy.lowercase()) {
            "plate" -> "v.plate"
            "model" -> "v.model"
            "brand" -> "v.brand"
            "year" -> "v.year"
            "status" -> "v.status"
            else -> "v.id"
        }
        val orderDirection = if (sortOrder == SortOrder.ASC) "ASC" else "DESC"

        // Construção dos filtros dinâmicos
        val filterConditions = mutableListOf<String>()
        filters.forEach { (key, value) ->
            when (key.lowercase()) {
                "plate" -> if (value is String && value.isNotBlank()) {
                    filterConditions.add("v.plate LIKE '%$value%'")
                }
                "model" -> if (value is String && value.isNotBlank()) {
                    filterConditions.add("v.model LIKE '%$value%'")
                }
                "brand" -> if (value is String && value.isNotBlank()) {
                    filterConditions.add("v.brand LIKE '%$value%'")
                }
                "year" -> if (value is Int) {
                    filterConditions.add("v.year = $value")
                }
                "status" -> if (value is String && value in listOf("ATIVO", "INATIVO", "MANUTENCAO")) {
                    filterConditions.add("v.status = '$value'")
                }
            }
        }
        val filterClause = if (filterConditions.isNotEmpty()) {
            "AND ${filterConditions.joinToString(" AND ")}"
        } else {
            ""
        }

        val sql = """
            SELECT 
                v.id,
                v.plate,
                v.model,
                v.brand,
                v.year,
                v.status,
                v.icon_map_url,
                v.deleted_at,
                (SELECT COUNT(*) 
                 FROM vehicles v2 
                 INNER JOIN trips t2 ON v2.id = t2.vehicle_id 
                 WHERE t2.driver_id = $driverId 
                 AND v2.deleted_at IS NULL $filterClause) AS total_count
            FROM vehicles v
            INNER JOIN trips t ON v.id = t.vehicle_id
            WHERE t.driver_id = $driverId 
            AND v.deleted_at IS NULL
            AND EXISTS (
                SELECT 1 
                FROM drivers d 
                WHERE d.id = $driverId 
                AND d.deleted_at IS NULL
            )
            $filterClause
            ORDER BY $sortColumn $orderDirection
            LIMIT $limit OFFSET ${(page - 1) * limit}
        """.trimIndent()

        var results = emptyList<Vehicle>()
        var total = 0

        transaction {
            exec(sql) { rs ->
                val vehicles = mutableListOf<Vehicle>()
                while (rs.next()) {
                    val statusStr = rs.getString("status")
                    val status = try {
                        VehicleStatus.valueOf(statusStr)
                    } catch (e: IllegalArgumentException) {
                        VehicleStatus.ATIVO // Valor padrão em caso de status inválido
                    }
                    vehicles.add(
                        Vehicle(
                            id = rs.getInt("id"),
                            plate = rs.getString("plate"),
                            model = rs.getString("model"),
                            brand = rs.getString("brand"),
                            year = rs.getInt("year"),
                            status = status,
                            iconMapUrl = rs.getString("icon_map_url"),
                        )
                    )
                    total = rs.getInt("total_count")
                }
                results = vehicles
            }
        }

        // Verifica se o motorista existe (se total_count é 0, pode ser porque o motorista não existe)
        if (total == 0 && results.isEmpty()) {
            val driverExists = transaction {
                DriversTable.selectAll()
                    .where { DriversTable.id eq driverId and DriversTable.deletedAt.isNull() }
                    .count() > 0
            }
            if (!driverExists) {
                return@dbQuery ServiceResponse(
                    status = HttpStatusCode.NotFound,
                    data = PaginatedResponse(
                        data = emptyList(),
                        total = 0,
                        page = page,
                        limit = limit,
                        totalPages = 0
                    )
                )
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = PaginatedResponse(
                data = results,
                total = total,
                page = page,
                limit = limit,
                totalPages = if (total == 0) 0 else ((total + limit - 1) / limit)
            )
        )
    }

    suspend fun getExpensesByDriver(
        driverId: Int,
        page: Int = 1,
        limit: Int = 5,
        sortBy: String = "date",
        sortOrder: SortOrder = SortOrder.DESC,
        filters: Map<String, Any> = emptyMap()
    ): ServiceResponse<PaginatedResponse<Expense>> = DatabaseFactory.dbQuery {
        val sortColumn = when (sortBy.lowercase()) {
            "type" -> "e.type"
            "amount" -> "e.amount"
            "liters" -> "e.liters"
            "date" -> "e.date"
            "vehicle_id" -> "e.vehicle_id"
            else -> "e.id"
        }
        val orderDirection = if (sortOrder == SortOrder.ASC) "ASC" else "DESC"

        val filterConditions = filters.mapNotNull { (key, value) ->
            when (key.lowercase()) {
                "type" -> (value as? String)?.takeIf { it.isNotBlank() }?.let { "e.type LIKE '%$it%'" }
                "amount" -> (value as? Double)?.let { "e.amount = $it" }
                "liters" -> (value as? Double)?.let { "e.liters = $it" }
                "date" -> (value as? String)?.takeIf { it.isNotBlank() }?.let {
                    runCatching { kotlinx.datetime.LocalDateTime.parse(it) }.getOrNull()?.let { _ ->
                        "e.date = '$it'"
                    }
                }
                "vehicle_id" -> (value as? Int)?.let { "e.vehicle_id = $it" }
                else -> null
            }
        }

        val filterClause = if (filterConditions.isNotEmpty()) {
            "AND ${filterConditions.joinToString(" AND ")}"
        } else ""

        // SQL principal
        val sql = """
        SELECT 
            e.id,
            e.type,
            e.amount,
            e.liters,
            e.date,
            e.vehicle_id,
            e.driver_id,
            (SELECT COUNT(*) 
             FROM expenses e2 
             WHERE e2.driver_id = $driverId $filterClause) AS total_count
        FROM expenses e
        WHERE e.driver_id = $driverId
        $filterClause
        ORDER BY $sortColumn $orderDirection
        LIMIT $limit OFFSET ${(page - 1) * limit}
    """.trimIndent()

        var results = emptyList<Expense>()
        var total = 0

        transaction {
            exec(sql) { rs ->
                val expenses = mutableListOf<Expense>()
                while (rs.next()) {
                    expenses.add(
                        Expense(
                            id = rs.getInt("id"),
                            type = rs.getString("type"),
                            amount = rs.getDouble("amount"),
                            liters = rs.getDouble("liters")?.takeIf { !it.isNaN() },
                            date = rs.getTimestamp("date").toLocalDateTime().toLocalDate().toKotlinLocalDate(),
                            vehicleId = rs.getInt("vehicle_id").takeIf { !rs.wasNull() },
                            driverId = rs.getInt("driver_id").takeIf { !rs.wasNull() },
                        )
                    )
                    total = rs.getInt("total_count")
                }
                results = expenses
            }
        }

        // Verifica se o motorista existe (caso total seja 0)
        if (total == 0 && results.isEmpty()) {
            val driverExists = transaction {
                DriversTable.selectAll()
                    .where { DriversTable.id eq driverId and DriversTable.deletedAt.isNull() }
                    .count() > 0
            }
            if (!driverExists) {
                return@dbQuery ServiceResponse(
                    status = HttpStatusCode.NotFound,
                    data = PaginatedResponse(
                        data = emptyList(),
                        total = 0,
                        page = page,
                        limit = limit,
                        totalPages = 0
                    )
                )
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = PaginatedResponse(
                data = results,
                total = total,
                page = page,
                limit = limit,
                totalPages = if (total == 0) 0 else ((total + limit - 1) / limit)
            )
        )
    }
}
