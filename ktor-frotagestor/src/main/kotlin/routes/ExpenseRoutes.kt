package com.frotagestor.routes

import com.frotagestor.controllers.ExpenseController
import com.frotagestor.services.ExpenseService
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*

fun Route.expenseRoutes() {
    val controller = ExpenseController(ExpenseService())

    authenticate("auth-jwt") {
        route("expenses") {
            get { controller.getAll(call) }
            post { controller.create(call) }
            get("{id}") { controller.getById(call) }
            patch("{id}") { controller.update(call) }
            delete("{id}") { controller.delete(call) }
            get("refueling-indicators") { controller.getRefuelingIndicators(call) }
            get("maintenance-indicators") { controller.getMaintenanceIndicators(call) }
            get("expense-indicators") { controller.getExpenseIndicators(call) }
        }
    }
}
