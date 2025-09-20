package com.redenorte.services

class UserService {
    fun login(username: String, password: String): ServiceResult {
        // regra de negócio de login
        return ServiceResult("SUCCESS", mapOf("token" to "fake-jwt-token"))
    }

    fun create(request: CreateUserRequest): ServiceResult {
        // regra de cadastro
        return ServiceResult("CREATED", mapOf("message" to "Usuário criado com sucesso"))
    }

    fun getAll(): ServiceResult {
        return ServiceResult("SUCCESS", listOf(mapOf("id" to 1, "username" to "teste")))
    }

    fun updateUser(id: Int, request: UpdateUserRequest): ServiceResult {
        return ServiceResult("SUCCESS", mapOf("message" to "Usuário $id atualizado"))
    }

    fun deleteUser(id: Int): ServiceResult {
        return ServiceResult("SUCCESS", mapOf("message" to "Usuário $id deletado"))
    }
}
