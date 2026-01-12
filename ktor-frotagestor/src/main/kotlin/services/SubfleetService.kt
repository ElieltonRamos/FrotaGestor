package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.*
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateSubfleet
import com.frotagestor.validations.validatePartialSubfleet
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class SubfleetService {

    /**
     * Criar nova subfrota
     */
    suspend fun createSubfleet(req: String): ServiceResponse<Subfleet> {
        val newSubfleet = validateSubfleet(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg) as Subfleet
            )
        }

        // Verificar se nome já existe
        val existingSubfleet = DatabaseFactory.dbQuery {
            SubfleetsTable
                .selectAll()
                .where { SubfleetsTable.name eq newSubfleet.name }
                .singleOrNull()
        }

        if (existingSubfleet != null) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Subfrota com este nome já existe!") as Subfleet
            )
        }

        // Validar parentId (se informado)
        if (newSubfleet.parentId != null) {
            val parentExists = DatabaseFactory.dbQuery {
                SubfleetsTable
                    .selectAll()
                    .where {
                        (SubfleetsTable.id eq newSubfleet.parentId) and
                                (SubfleetsTable.status eq SubfleetStatus.ACTIVE)
                    }
                    .singleOrNull()
            }

            if (parentExists == null) {
                return ServiceResponse(
                    status = HttpStatusCode.BadRequest,
                    data = Message("Subfrota pai não encontrada ou está inativa!") as Subfleet
                )
            }
        }

        // Validar managerUserId (se informado)
        if (newSubfleet.managerUserId != null) {
            val managerExists = DatabaseFactory.dbQuery {
                UsersTable
                    .selectAll()
                    .where { UsersTable.id eq newSubfleet.managerUserId }
                    .singleOrNull()
            }

            if (managerExists == null) {
                return ServiceResponse(
                    status = HttpStatusCode.BadRequest,
                    data = Message("Usuário gerente não encontrado!") as Subfleet
                )
            }
        }

        val subfleetId = DatabaseFactory.dbQuery {
            SubfleetsTable.insert {
                it[name] = newSubfleet.name
                it[description] = newSubfleet.description
                it[parentId] = newSubfleet.parentId
                it[color] = newSubfleet.color
                it[icon] = newSubfleet.icon
                it[managerUserId] = newSubfleet.managerUserId
                it[status] = newSubfleet.status
            }[SubfleetsTable.id]
        }

        return findSubfleetById(subfleetId)
    }

    /**
     * Atualizar subfrota
     */
    suspend fun updateSubfleet(id: Int, req: String): ServiceResponse<Message> {
        val updatedSubfleet = validatePartialSubfleet(req).getOrReturn { msg ->
            return ServiceResponse(
                status = HttpStatusCode.BadRequest,
                data = Message(msg)
            )
        }

        val existingSubfleet = DatabaseFactory.dbQuery {
            SubfleetsTable
                .selectAll()
                .where { SubfleetsTable.id eq id }
                .singleOrNull()
        }

        if (existingSubfleet == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Subfrota não encontrada!")
            )
        }

        // Validar nome único (se alterado)
        if (updatedSubfleet.name != null) {
            val nameExists = DatabaseFactory.dbQuery {
                SubfleetsTable
                    .selectAll()
                    .where {
                        (SubfleetsTable.name eq updatedSubfleet.name) and
                                (SubfleetsTable.id neq id)
                    }
                    .singleOrNull()
            }

            if (nameExists != null) {
                return ServiceResponse(
                    status = HttpStatusCode.Conflict,
                    data = Message("Já existe outra subfrota com este nome!")
                )
            }
        }

        // Validar parentId (se alterado)
        if (updatedSubfleet.parentId != null) {
            // Não pode ser pai de si mesmo
            if (updatedSubfleet.parentId == id) {
                return ServiceResponse(
                    status = HttpStatusCode.BadRequest,
                    data = Message("Uma subfrota não pode ser pai dela mesma!")
                )
            }

            val parentExists = DatabaseFactory.dbQuery {
                SubfleetsTable
                    .selectAll()
                    .where { SubfleetsTable.id eq updatedSubfleet.parentId }
                    .singleOrNull()
            }

            if (parentExists == null) {
                return ServiceResponse(
                    status = HttpStatusCode.BadRequest,
                    data = Message("Subfrota pai não encontrada!")
                )
            }
        }

        DatabaseFactory.dbQuery {
            SubfleetsTable.update({ SubfleetsTable.id eq id }) {
                updatedSubfleet.name?.let { n -> it[name] = n }
                updatedSubfleet.description?.let { d -> it[description] = d }
                updatedSubfleet.parentId?.let { p -> it[parentId] = p }
                updatedSubfleet.color?.let { c -> it[color] = c }
                updatedSubfleet.icon?.let { i -> it[icon] = i }
                updatedSubfleet.managerUserId?.let { m -> it[managerUserId] = m }
                updatedSubfleet.status?.let { s -> it[status] = s }
            }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Subfrota atualizada com sucesso")
        )
    }

    /**
     * Listar subfrotas com filtros e paginação
     */
    suspend fun getAllSubfleets(
        page: Int = 1,
        limit: Int = 10,
        statusFilter: SubfleetStatus? = null,
        parentIdFilter: Int? = null,
        managerUserIdFilter: Int? = null
    ): ServiceResponse<PaginatedResponse<Subfleet>> {
        return DatabaseFactory.dbQuery {
            // Alias para self-join (parent)
            val parentTable = SubfleetsTable.alias("parent")

            val query = SubfleetsTable
                .leftJoin(parentTable, { parentId }, { parentTable[SubfleetsTable.id] })
                .leftJoin(UsersTable, { SubfleetsTable.managerUserId }, { UsersTable.id })
                .selectAll()
                .apply {
                    if (statusFilter != null) {
                        andWhere { SubfleetsTable.status eq statusFilter }
                    }
                    if (parentIdFilter != null) {
                        andWhere { SubfleetsTable.parentId eq parentIdFilter }
                    }
                    if (managerUserIdFilter != null) {
                        andWhere { SubfleetsTable.managerUserId eq managerUserIdFilter }
                    }
                }

            // Contar total ANTES de aplicar paginação
            val total = query.count()

            // Aplicar paginação e ordenação
            val results = query
                .orderBy(SubfleetsTable.name to SortOrder.ASC)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map { row ->
                    val subfleetId = row[SubfleetsTable.id]

                    // Contar veículos ativos
                    val vehicleCount = VehiclesTable
                        .selectAll()
                        .where {
                            (VehiclesTable.subfleetId eq subfleetId) and
                                    (VehiclesTable.deletedAt.isNull())
                        }
                        .count()

                    val activeVehicleCount = VehiclesTable
                        .selectAll()
                        .where {
                            (VehiclesTable.subfleetId eq subfleetId) and
                                    (VehiclesTable.status eq VehicleStatus.ATIVO) and
                                    (VehiclesTable.deletedAt.isNull())
                        }
                        .count()

                    Subfleet(
                        id = subfleetId,
                        name = row[SubfleetsTable.name],
                        description = row[SubfleetsTable.description],
                        parentId = row[SubfleetsTable.parentId],
                        parentName = row.getOrNull(parentTable[SubfleetsTable.name]),
                        color = row[SubfleetsTable.color],
                        icon = row[SubfleetsTable.icon],
                        managerUserId = row[SubfleetsTable.managerUserId],
                        managerName = row.getOrNull(UsersTable.username),
                        status = row[SubfleetsTable.status],
                        vehicleCount = vehicleCount.toInt(),
                        activeVehicleCount = activeVehicleCount.toInt(),
                        createdAt = row[SubfleetsTable.createdAt]
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

    /**
     * Buscar subfrota por ID
     */
    suspend fun findSubfleetById(id: Int): ServiceResponse<Subfleet> {
        val subfleet = DatabaseFactory.dbQuery {
            val parentTable = SubfleetsTable.alias("parent")

            SubfleetsTable
                .leftJoin(parentTable, { parentId }, { parentTable[SubfleetsTable.id] })
                .leftJoin(UsersTable, { SubfleetsTable.managerUserId }, { UsersTable.id })
                .selectAll()
                .where { SubfleetsTable.id eq id }
                .singleOrNull()?.let { row ->
                    // Contar veículos
                    val vehicleCount = VehiclesTable
                        .selectAll()
                        .where {
                            (VehiclesTable.subfleetId eq id) and
                                    (VehiclesTable.deletedAt.isNull())
                        }
                        .count()

                    val activeVehicleCount = VehiclesTable
                        .selectAll()
                        .where {
                            (VehiclesTable.subfleetId eq id) and
                                    (VehiclesTable.status eq VehicleStatus.ATIVO) and
                                    (VehiclesTable.deletedAt.isNull())
                        }
                        .count()

                    Subfleet(
                        id = row[SubfleetsTable.id],
                        name = row[SubfleetsTable.name],
                        description = row[SubfleetsTable.description],
                        parentId = row[SubfleetsTable.parentId],
                        parentName = row.getOrNull(parentTable[SubfleetsTable.name]),
                        color = row[SubfleetsTable.color],
                        icon = row[SubfleetsTable.icon],
                        managerUserId = row[SubfleetsTable.managerUserId],
                        managerName = row.getOrNull(UsersTable.username),
                        status = row[SubfleetsTable.status],
                        vehicleCount = vehicleCount.toInt(),
                        activeVehicleCount = activeVehicleCount.toInt(),
                        createdAt = row[SubfleetsTable.createdAt]
                    )
                }
        }

        return if (subfleet == null) {
            ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Subfrota não encontrada") as Subfleet
            )
        } else {
            ServiceResponse(HttpStatusCode.OK, subfleet)
        }
    }

    /**
     * Deletar subfrota
     */
    suspend fun deleteSubfleet(id: Int): ServiceResponse<Message> {
        val existingSubfleet = DatabaseFactory.dbQuery {
            SubfleetsTable
                .selectAll()
                .where { SubfleetsTable.id eq id }
                .singleOrNull()
        }

        if (existingSubfleet == null) {
            return ServiceResponse(
                status = HttpStatusCode.NotFound,
                data = Message("Subfrota não encontrada!")
            )
        }

        // Verificar se tem veículos associados
        val hasVehicles = DatabaseFactory.dbQuery {
            VehiclesTable
                .selectAll()
                .where { VehiclesTable.subfleetId eq id }
                .count() > 0
        }

        if (hasVehicles) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Não é possível deletar subfrota com veículos associados. Remova ou transfira os veículos primeiro.")
            )
        }

        // Verificar se tem subfrotas filhas
        val hasChildren = DatabaseFactory.dbQuery {
            SubfleetsTable
                .selectAll()
                .where { SubfleetsTable.parentId eq id }
                .count() > 0
        }

        if (hasChildren) {
            return ServiceResponse(
                status = HttpStatusCode.Conflict,
                data = Message("Não é possível deletar subfrota que possui subfrotas filhas.")
            )
        }

        DatabaseFactory.dbQuery {
            SubfleetsTable.deleteWhere { SubfleetsTable.id eq id }
        }

        return ServiceResponse(
            status = HttpStatusCode.OK,
            data = Message("Subfrota deletada com sucesso")
        )
    }

    /**
     * Relatório de subfrota (opcional)
     */
    suspend fun getSubfleetReport(subfleetId: Int): ServiceResponse<SubfleetReport> {
        val report = DatabaseFactory.dbQuery {
            // Buscar veículos da subfrota
            val vehicles = VehiclesTable
                .selectAll()
                .where {
                    (VehiclesTable.subfleetId eq subfleetId) and
                            (VehiclesTable.deletedAt.isNull())
                }
                .toList()

            val vehicleIds = vehicles.map { it[VehiclesTable.id] }

            val totalVehicles = vehicles.size
            val activeVehicles = vehicles.count { it[VehiclesTable.status] == VehicleStatus.ATIVO }
            val maintenanceVehicles = vehicles.count { it[VehiclesTable.status] == VehicleStatus.MANUTENCAO }

            // Viagens últimos 30 dias
            val thirtyDaysAgo = Clock.System.now()
                .minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val trips = if (vehicleIds.isNotEmpty()) {
                TripsTable
                    .selectAll()
                    .where {
                        (TripsTable.vehicleId inList vehicleIds) and
                                (TripsTable.startTime greaterEq thirtyDaysAgo)
                    }
                    .toList()
            } else emptyList()

            val totalTrips = trips.size
            val totalDistance = trips.sumOf { it[TripsTable.distanceKm]?.toDouble() ?: 0.0 }

            // Despesas últimos 30 dias
            val expenses = if (vehicleIds.isNotEmpty()) {
                ExpensesTable
                    .selectAll()
                    .where {
                        (ExpensesTable.vehicleId inList vehicleIds) and
                                (ExpensesTable.date greaterEq thirtyDaysAgo.date)
                    }
                    .toList()
            } else emptyList()

            val totalExpenses = expenses.sumOf { it[ExpensesTable.amount].toDouble() }

            SubfleetReport(
                subfleetId = subfleetId,
                totalVehicles = totalVehicles,
                activeVehicles = activeVehicles,
                maintenanceVehicles = maintenanceVehicles,
                totalTrips = totalTrips,
                totalDistanceKm = totalDistance,
                totalExpenses = totalExpenses
            )
        }

        return ServiceResponse(HttpStatusCode.OK, report)
    }
}