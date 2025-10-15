package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.DriversTable
import com.frotagestor.database.models.ExpensesTable
import com.frotagestor.database.models.VehiclesTable
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateExpense
import com.frotagestor.validations.validatePartialExpense
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ExpenseService {

    suspend fun createExpense(req: String): ServiceResponse<Message> {
        val newExpense = validateExpense(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        DatabaseFactory.dbQuery {
            ExpensesTable.insert {
                it[vehicleId] = newExpense.vehicleId
                it[driverId] = newExpense.driverId
                it[tripId] = newExpense.tripId
                it[date] = newExpense.date
                it[type] = newExpense.type
                it[description] = newExpense.description
                it[amount] = newExpense.amount.toBigDecimal()
                // 🔹 novos campos de abastecimento
                it[liters] = newExpense.liters?.toBigDecimal()
                it[pricePerLiter] = newExpense.pricePerLiter?.toBigDecimal()
                it[odometer] = newExpense.odometer
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.Created,
            data = Message("Despesa criada com sucesso")
        )
    }

    suspend fun updateExpense(id: Int, req: String): ServiceResponse<Message> {
        val updatedExpense = validatePartialExpense(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingExpense = DatabaseFactory.dbQuery {
            ExpensesTable
                .selectAll()
                .where { ExpensesTable.id eq id }
                .singleOrNull()
        }

        if (existingExpense == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Despesa não encontrada!")
            )
        }

        DatabaseFactory.dbQuery {
            ExpensesTable.update({ ExpensesTable.id eq id }) {
                updatedExpense.vehicleId?.let { v -> it[vehicleId] = v }
                updatedExpense.driverId?.let { d -> it[driverId] = d }
                updatedExpense.tripId?.let { t -> it[tripId] = t }
                updatedExpense.date?.let { dt -> it[date] = dt }
                updatedExpense.type?.let { tp -> it[type] = tp }
                updatedExpense.description?.let { desc -> it[description] = desc }
                updatedExpense.amount?.let { am -> it[amount] = am.toBigDecimal() }
                // 🔹 novos campos de abastecimento
                updatedExpense.liters?.let { l -> it[liters] = l.toBigDecimal() }
                updatedExpense.pricePerLiter?.let { ppl -> it[pricePerLiter] = ppl.toBigDecimal() }
                updatedExpense.odometer?.let { odo -> it[odometer] = odo }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Despesa atualizada com sucesso")
        )
    }

    suspend fun getAllExpenses(
        page: Int = 1,
        limit: Int = 10,
        sortBy: Column<*> = ExpensesTable.id,
        sortOrder: SortOrder = SortOrder.ASC,
        idFilter: Int? = null,
        vehicleIdFilter: Int? = null,
        driverIdFilter: Int? = null,
        tripIdFilter: Int? = null,
        typeFilter: String? = null,
        dateStartFilter: LocalDate? = null,
        dateEndFilter: LocalDate? = null,
        descriptionFilter: String? = null,
        minAmountFilter: Double? = null,
        maxAmountFilter: Double? = null,
        minLitersFilter: Double? = null,
        maxLitersFilter: Double? = null,
        minOdometerFilter: Int? = null,
        maxOdometerFilter: Int? = null,
        minPricePerLiterFilter: Double? = null,
        maxPricePerLiterFilter: Double? = null,
        driverNameFilter: String? = null,
        vehiclePlateFilter: String? = null
    ): ServiceResponse<PaginatedResponse<Expense>> {
        return DatabaseFactory.dbQuery {
            val query = ExpensesTable
                .join(DriversTable, JoinType.LEFT, additionalConstraint = { ExpensesTable.driverId eq DriversTable.id })
                .join(VehiclesTable, JoinType.LEFT, additionalConstraint = { ExpensesTable.vehicleId eq VehiclesTable.id })
                .selectAll()
                .apply {
                    if (idFilter != null) andWhere { ExpensesTable.id eq idFilter }
                    if (vehicleIdFilter != null) andWhere { ExpensesTable.vehicleId eq vehicleIdFilter }
                    if (driverIdFilter != null) andWhere { ExpensesTable.driverId eq driverIdFilter }
                    if (tripIdFilter != null) andWhere { ExpensesTable.tripId eq tripIdFilter }
                    if (!typeFilter.isNullOrBlank()) andWhere { ExpensesTable.type like "%$typeFilter%" }
                    if (!descriptionFilter.isNullOrBlank()) andWhere { ExpensesTable.description like "%$descriptionFilter%" }
                    if (dateStartFilter != null) andWhere { ExpensesTable.date greaterEq dateStartFilter }
                    if (dateEndFilter != null) andWhere { ExpensesTable.date lessEq dateEndFilter }

                    if (minAmountFilter != null) andWhere { ExpensesTable.amount greaterEq minAmountFilter.toBigDecimal() }
                    if (maxAmountFilter != null) andWhere { ExpensesTable.amount lessEq maxAmountFilter.toBigDecimal() }
                    if (minLitersFilter != null) andWhere { ExpensesTable.liters greaterEq minLitersFilter.toBigDecimal() }
                    if (maxLitersFilter != null) andWhere { ExpensesTable.liters lessEq maxLitersFilter.toBigDecimal() }
                    if (minOdometerFilter != null) andWhere { ExpensesTable.odometer greaterEq minOdometerFilter }
                    if (maxOdometerFilter != null) andWhere { ExpensesTable.odometer lessEq maxOdometerFilter }
                    if (minPricePerLiterFilter != null) andWhere { ExpensesTable.pricePerLiter greaterEq minPricePerLiterFilter.toBigDecimal() }
                    if (maxPricePerLiterFilter != null) andWhere { ExpensesTable.pricePerLiter lessEq maxPricePerLiterFilter.toBigDecimal() }
                    if (!driverNameFilter.isNullOrBlank()) andWhere { DriversTable.name like "%$driverNameFilter%" }
                    if (!vehiclePlateFilter.isNullOrBlank()) andWhere { VehiclesTable.plate like "%$vehiclePlateFilter%" }
                }

            val total = query.count()

            val results = query
                .orderBy(sortBy to sortOrder)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map {
                    Expense(
                        id = it[ExpensesTable.id],
                        vehicleId = it[ExpensesTable.vehicleId],
                        driverId = it[ExpensesTable.driverId],
                        tripId = it[ExpensesTable.tripId],
                        date = it[ExpensesTable.date],
                        type = it[ExpensesTable.type],
                        description = it[ExpensesTable.description],
                        amount = it[ExpensesTable.amount].toDouble(),
                        liters = it[ExpensesTable.liters]?.toDouble(),
                        pricePerLiter = it[ExpensesTable.pricePerLiter]?.toDouble(),
                        odometer = it[ExpensesTable.odometer],
                        driverName = it[DriversTable.name],
                        vehiclePlate = it[VehiclesTable.plate]
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



    suspend fun findExpenseById(id: Int): ServiceResponse<Any> {
        val expense = DatabaseFactory.dbQuery {
            ExpensesTable
                .join(DriversTable, JoinType.LEFT, additionalConstraint = { ExpensesTable.driverId eq DriversTable.id })
                .join(VehiclesTable, JoinType.LEFT, additionalConstraint = { ExpensesTable.vehicleId eq VehiclesTable.id })
                .selectAll()
                .where { ExpensesTable.id eq id }
                .singleOrNull()
                ?.let {
                    Expense(
                        id = it[ExpensesTable.id],
                        vehicleId = it[ExpensesTable.vehicleId],
                        driverId = it[ExpensesTable.driverId],
                        tripId = it[ExpensesTable.tripId],
                        date = it[ExpensesTable.date],
                        type = it[ExpensesTable.type],
                        description = it[ExpensesTable.description],
                        amount = it[ExpensesTable.amount].toDouble(),
                        liters = it[ExpensesTable.liters]?.toDouble(),
                        pricePerLiter = it[ExpensesTable.pricePerLiter]?.toDouble(),
                        odometer = it[ExpensesTable.odometer],
                        driverName = it[DriversTable.name],
                        vehiclePlate = it[VehiclesTable.plate]
                    )
                }
        }

        return if (expense == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = mapOf("message" to "Despesa não encontrada")
            )
        } else {
            ServiceResponse(
                status = HttpStatusCode.OK,
                data = expense
            )
        }
    }

    suspend fun deleteExpense(id: Int): ServiceResponse<Message> {
        val existingExpense = DatabaseFactory.dbQuery {
            ExpensesTable
                .selectAll()
                .where { ExpensesTable.id eq id }
                .singleOrNull()
        }

        if (existingExpense == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Despesa não encontrada!")
            )
        }

        DatabaseFactory.dbQuery {
            ExpensesTable.deleteWhere { ExpensesTable.id eq id }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Despesa removida com sucesso")
        )
    }

    suspend fun getRefuelingIndicators(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<RefuelingIndicators> = DatabaseFactory.dbQuery {
        // Define datas padrão: mês atual
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val tz = TimeZone.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(tz).toLocalDateTime(tz)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(tz).toLocalDateTime(tz)

        val sql = """
        SELECT
            COUNT(e.id) AS total_count,
            COALESCE(SUM(e.amount),0) AS total_amount,
            COALESCE(SUM(e.liters),0) AS total_liters,
            COALESCE(AVG(e.price_per_liter),0) AS avg_price_per_liter,
            (SELECT d.name FROM drivers d
             JOIN expenses e2 ON d.id = e2.driver_id
             WHERE e2.type = 'Combustivel' AND e2.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY d.id ORDER BY COUNT(*) DESC LIMIT 1) AS top_driver_name,
            (SELECT COUNT(*) FROM expenses e3
             JOIN drivers d3 ON d3.id = e3.driver_id
             WHERE e3.type = 'Combustivel' AND e3.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY e3.driver_id ORDER BY COUNT(*) DESC LIMIT 1) AS top_driver_count,
            (SELECT v.plate FROM vehicles v
             JOIN expenses e4 ON v.id = e4.vehicle_id
             WHERE e4.type = 'Combustivel' AND e4.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v.id ORDER BY SUM(e4.amount) DESC LIMIT 1) AS top_vehicle_amount_plate,
            (SELECT SUM(e5.amount) FROM expenses e5
             JOIN vehicles v5 ON v5.id = e5.vehicle_id
             WHERE e5.type = 'Combustivel' AND e5.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v5.id ORDER BY SUM(e5.amount) DESC LIMIT 1) AS top_vehicle_amount,
            (SELECT v.plate FROM vehicles v
             JOIN expenses e6 ON v.id = e6.vehicle_id
             WHERE e6.type = 'Combustivel' AND e6.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v.id ORDER BY SUM(e6.liters) DESC LIMIT 1) AS top_vehicle_liters_plate,
            (SELECT SUM(e7.liters) FROM expenses e7
             JOIN vehicles v7 ON v7.id = e7.vehicle_id
             WHERE e7.type = 'Combustivel' AND e7.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v7.id ORDER BY SUM(e7.liters) DESC LIMIT 1) AS top_vehicle_liters,
            (SELECT e8.date FROM expenses e8
             WHERE e8.type = 'Combustivel' AND e8.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e8.date DESC LIMIT 1) AS last_refueling_date,
            (SELECT v8.plate FROM vehicles v8
             JOIN expenses e9 ON v8.id = e9.vehicle_id
             WHERE e9.type = 'Combustivel' AND e9.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e9.date DESC LIMIT 1) AS last_refueling_plate
        FROM expenses e
        WHERE e.type = 'Combustivel' AND e.date BETWEEN '$startDateTime' AND '$endDateTime'
    """.trimIndent()

        var indicators: RefuelingIndicators? = null

        transaction {
            exec(sql) { rs ->
                if (rs.next()) {
                    indicators = RefuelingIndicators(
                        totalAmount = rs.getDouble("total_amount"),
                        totalLiters = rs.getDouble("total_liters"),
                        avgPricePerLiter = rs.getDouble("avg_price_per_liter"),
                        topDriver = rs.getString("top_driver_name")?.let { name ->
                            RefuelingIndicators.TopDriver(name, rs.getInt("top_driver_count"))
                        },
                        topVehicleByAmount = rs.getString("top_vehicle_amount_plate")?.let { plate ->
                            RefuelingIndicators.TopVehicleAmount(plate, rs.getDouble("top_vehicle_amount"))
                        },
                        topVehicleByLiters = rs.getString("top_vehicle_liters_plate")?.let { plate ->
                            RefuelingIndicators.TopVehicleLiters(plate, rs.getDouble("top_vehicle_liters"))
                        },
                        lastRefueling = rs.getString("last_refueling_date")?.let { dateStr ->
                            RefuelingIndicators.LastRefueling(
                                date = LocalDate.parse(dateStr).toString(),
                                plate = rs.getString("last_refueling_plate")
                            )
                        }
                    )
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = indicators ?: RefuelingIndicators(
                totalAmount = 0.0,
                totalLiters = 0.0,
                avgPricePerLiter = 0.0
            )
        )
    }

    suspend fun getMaintenanceIndicators(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<MaintenanceIndicators> = DatabaseFactory.dbQuery {
        // Define datas padrão: mês atual
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val tz = TimeZone.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(tz).toLocalDateTime(tz)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(tz).toLocalDateTime(tz)

        val sql = """
        SELECT
            COUNT(e.id) AS total_count,
            COALESCE(SUM(e.amount), 0) AS total_amount,
            (SELECT description FROM expenses e2
             WHERE e2.type = 'Manutencao' AND e2.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY e2.description ORDER BY COUNT(*) DESC LIMIT 1) AS most_common_type,
            (SELECT v.plate FROM vehicles v
             JOIN expenses e3 ON v.id = e3.vehicle_id
             WHERE e3.type = 'Manutencao' AND e3.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v.id ORDER BY SUM(e3.amount) DESC LIMIT 1) AS top_vehicle_plate,
            (SELECT SUM(e4.amount) FROM expenses e4
             JOIN vehicles v4 ON v4.id = e4.vehicle_id
             WHERE e4.type = 'Manutencao' AND e4.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY v4.id ORDER BY SUM(e4.amount) DESC LIMIT 1) AS top_vehicle_amount,
            (SELECT e5.date FROM expenses e5
             WHERE e5.type = 'Manutencao' AND e5.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e5.date DESC LIMIT 1) AS last_maintenance_date,
            (SELECT v6.plate FROM vehicles v6
             JOIN expenses e7 ON v6.id = e7.vehicle_id
             WHERE e7.type = 'Manutencao' AND e7.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e7.date DESC LIMIT 1) AS last_maintenance_plate
        FROM expenses e
        WHERE e.type = 'Manutencao' AND e.date BETWEEN '$startDateTime' AND '$endDateTime'
    """.trimIndent()

        var indicators: MaintenanceIndicators? = null

        transaction {
            exec(sql) { rs ->
                if (rs.next()) {
                    indicators = MaintenanceIndicators(
                        totalAmount = rs.getDouble("total_amount"),
                        totalCount = rs.getInt("total_count"),
                        mostCommonType = rs.getString("most_common_type") ?: "",
                        topVehicleByAmount = rs.getString("top_vehicle_plate")?.let { plate ->
                            MaintenanceIndicators.TopVehicleAmount(
                                plate = plate,
                                amount = rs.getDouble("top_vehicle_amount")
                            )
                        } ?: MaintenanceIndicators.TopVehicleAmount(plate = "", amount = 0.0),
                        lastMaintenance = rs.getString("last_maintenance_date")?.let { dateStr ->
                            MaintenanceIndicators.LastMaintenance(
                                date = LocalDate.parse(dateStr),
                                plate = rs.getString("last_maintenance_plate") ?: ""
                            )
                        } ?: MaintenanceIndicators.LastMaintenance(
                            date = LocalDate(1900, 1, 1),
                            plate = ""
                        )
                    )
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = indicators ?: MaintenanceIndicators(
                totalAmount = 0.0,
                totalCount = 0,
                mostCommonType = "",
                topVehicleByAmount = MaintenanceIndicators.TopVehicleAmount(plate = "", amount = 0.0),
                lastMaintenance = MaintenanceIndicators.LastMaintenance(
                    date = LocalDate(1900, 1, 1),
                    plate = ""
                )
            )
        )
    }

    suspend fun getExpenseIndicators(
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): ServiceResponse<ExpenseIndicators> = DatabaseFactory.dbQuery {
        // Define datas padrão: mês atual
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = startDate ?: LocalDate(now.year, now.month, 1)
        val end = endDate ?: LocalDate(now.year, now.month, now.month.maxLength())
        val tz = TimeZone.currentSystemDefault()
        val startDateTime = start.atStartOfDayIn(tz).toLocalDateTime(tz)
        val endDateTime = end.plus(DatePeriod(days = 1)).atStartOfDayIn(tz).toLocalDateTime(tz)

        val sql = """
        SELECT
            COUNT(e.id) AS total_count,
            COALESCE(SUM(e.amount), 0) AS total_amount,
            (SELECT type FROM expenses e2
             WHERE e2.date BETWEEN '$startDateTime' AND '$endDateTime'
             GROUP BY e2.type ORDER BY COUNT(*) DESC LIMIT 1) AS most_common_type,
            (SELECT e3.date FROM expenses e3
             WHERE e3.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e3.date DESC LIMIT 1) AS last_expense_date,
            (SELECT e4.type FROM expenses e4
             WHERE e4.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e4.date DESC LIMIT 1) AS last_expense_type,
            (SELECT e5.description FROM expenses e5
             WHERE e5.date BETWEEN '$startDateTime' AND '$endDateTime'
             ORDER BY e5.date DESC LIMIT 1) AS last_expense_description
        FROM expenses e
        WHERE e.date BETWEEN '$startDateTime' AND '$endDateTime'
    """.trimIndent()

        var indicators: ExpenseIndicators? = null

        transaction {
            exec(sql) { rs ->
                if (rs.next()) {
                    indicators = ExpenseIndicators(
                        totalAmount = rs.getDouble("total_amount"),
                        totalCount = rs.getInt("total_count"),
                        mostCommonType = rs.getString("most_common_type"),
                        lastExpense = rs.getString("last_expense_date")?.let { dateStr ->
                            ExpenseIndicators.LastExpense(
                                date = LocalDate.parse(dateStr),
                                type = rs.getString("last_expense_type") ?: "",
                                description = rs.getString("last_expense_description") ?: ""
                            )
                        }
                    )
                }
            }
        }

        ServiceResponse(
            status = HttpStatusCode.OK,
            data = indicators ?: ExpenseIndicators(
                totalAmount = 0.0,
                totalCount = 0,
                mostCommonType = null,
                lastExpense = null
            )
        )
    }
}
