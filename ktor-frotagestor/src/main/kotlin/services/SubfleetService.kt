package com.frotagestor.services

import com.frotagestor.database.DatabaseFactory
import com.frotagestor.database.models.*
import com.frotagestor.interfaces.*
import com.frotagestor.validations.getOrReturn
import com.frotagestor.validations.validateSubfleet
import com.frotagestor.validations.validatePartialSubfleet
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

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

        val subfleetId = DatabaseFactory.dbQuery {
            SubfleetsTable.insert {
                it[name] = newSubfleet.name
                it[description] = newSubfleet.description
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

        DatabaseFactory.dbQuery {
            SubfleetsTable.update({ SubfleetsTable.id eq id }) {
                updatedSubfleet.name?.let { n -> it[name] = n }
                updatedSubfleet.description?.let { d -> it[description] = d }
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
        nameFilter: String? = null,
        descriptionFilter: String? = null
    ): ServiceResponse<PaginatedResponse<Subfleet>> {
        return DatabaseFactory.dbQuery {
            val query = SubfleetsTable
                .selectAll()
                .apply {
                    nameFilter?.let {
                        andWhere { SubfleetsTable.name like "%$it%" }
                    }
                    descriptionFilter?.let {
                        andWhere { SubfleetsTable.description like "%$it%" }
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

                    // Contar veículos
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
                        vehicleCount = vehicleCount.toInt(),
                        activeVehicleCount = activeVehicleCount.toInt()
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
     * NOVO: Indicadores do dashboard
     */
    suspend fun getIndicators(): ServiceResponse<SubfleetIndicators> {
        return DatabaseFactory.dbQuery {
            // Total subfrotas ativas (assumindo todas ativas por simplicidade)
            val totalActive = SubfleetsTable.selectAll().count()

            // Total veículos em todas subfrotas
            val totalVehicles = VehiclesTable
                .selectAll()
                .where { VehiclesTable.deletedAt.isNull() }
                .count()

            // Última subfrota (mais recente por ID)
            val lastSubfleetRow = SubfleetsTable
                .selectAll()
                .orderBy(SubfleetsTable.id to SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            val lastSubfleet = lastSubfleetRow?.let { row ->
                val subfleetId = row[SubfleetsTable.id]

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
                    vehicleCount = vehicleCount.toInt(),
                    activeVehicleCount = activeVehicleCount.toInt()
                )
            }

            val indicators = SubfleetIndicators(
                totalActive = totalActive.toInt(),
                totalVehicles = totalVehicles.toInt(),
                lastSubfleet = lastSubfleet
            )

            ServiceResponse(HttpStatusCode.OK, indicators)
        }
    }

    /**
     * Buscar subfrota por ID
     */
    suspend fun findSubfleetById(id: Int): ServiceResponse<Subfleet> {
        val subfleet = DatabaseFactory.dbQuery {
            SubfleetsTable
                .selectAll()
                .where { SubfleetsTable.id eq id }
                .singleOrNull()?.let { row ->
                    val subfleetId = row[SubfleetsTable.id]

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
                        id = subfleetId,
                        name = row[SubfleetsTable.name],
                        description = row[SubfleetsTable.description],
                        vehicleCount = vehicleCount.toInt(),
                        activeVehicleCount = activeVehicleCount.toInt()
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
     * Deletar subfrota (mantido)
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
                data = Message("Não é possível deletar subfrota com veículos associados.")
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

    suspend fun getVehiclesBySubfleet(
        subfleetId: Int,
        page: Int = 1,
        limit: Int = 10
    ): ServiceResponse<PaginatedResponse<Vehicle>> {
        return DatabaseFactory.dbQuery {
            val query = VehiclesTable
                .selectAll()
                .where {
                    (VehiclesTable.subfleetId eq subfleetId) and
                            (VehiclesTable.deletedAt.isNull())
                }

            val total = query.count()

            val results = query
                .orderBy(VehiclesTable.plate to SortOrder.ASC)
                .limit(limit, offset = ((page - 1) * limit).toLong())
                .map { row ->
                    Vehicle(  // Interface Vehicle simplificada
                        id = row[VehiclesTable.id],
                        plate = row[VehiclesTable.plate],
                        model = row[VehiclesTable.model],
                        brand = row[VehiclesTable.brand],
                        modelYear = row[VehiclesTable.modelYear],
                        manufacturingYear = row[VehiclesTable.manufacturingYear],
                        status = row[VehiclesTable.status],
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
}
