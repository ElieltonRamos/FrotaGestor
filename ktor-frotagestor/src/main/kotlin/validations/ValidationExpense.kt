package com.frotagestor.validations

import com.frotagestor.interfaces.Expense
import com.frotagestor.interfaces.PartialExpense
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun validateExpense(rawBody: String): ValidationResult<Expense> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val expense: Expense = try {
        Json.decodeFromString<Expense>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    val missingFields = mutableListOf<String>()
    if (expense.type.isBlank()) missingFields.add("type")
    if (expense.amount <= 0) missingFields.add("amount")
    if (expense.date == null) missingFields.add("date")

    return if (missingFields.isNotEmpty()) {
        val msg = if (missingFields.size == 1) {
            "O campo ${missingFields.first()} é obrigatório"
        } else {
            "Os campos ${missingFields.joinToString(", ")} são obrigatórios"
        }
        ValidationResult.Error(msg)
    } else {
        ValidationResult.Success(expense)
    }
}

fun validatePartialExpense(rawBody: String): ValidationResult<PartialExpense> {
    if (rawBody.isBlank()) {
        return ValidationResult.Error("Body da requisição está vazio")
    }

    val expense = try {
        Json.decodeFromString<PartialExpense>(rawBody)
    } catch (e: SerializationException) {
        return ValidationResult.Error("JSON inválido")
    }

    if (
        expense.vehicleId == null &&
        expense.driverId == null &&
        expense.tripId == null &&
        expense.date == null &&
        expense.type.isNullOrBlank() &&
        expense.description.isNullOrBlank() &&
        expense.amount == null
    ) {
        return ValidationResult.Error("Nenhum campo para atualizar foi fornecido")
    }

    return ValidationResult.Success(expense)
}
