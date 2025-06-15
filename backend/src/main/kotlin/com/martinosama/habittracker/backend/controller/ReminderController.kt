package com.martinosama.habittracker.backend.controller

import com.martinosama.habittracker.backend.dto.ReminderDto
import com.martinosama.habittracker.backend.mapper.toDto
import com.martinosama.habittracker.backend.service.ReminderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.reminderRoutes(service: ReminderService) {

    route("/reminder") {

        post {
            val dto = call.receive<ReminderDto>()
            val reminder = service.createReminder(dto)
            call.respond(reminder.toDto())
        }

        get("/{habitId}") {
            val habitId = call.parameters["habitId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing habitId")

            val reminders = service.getRemindersForHabit(UUID.fromString(habitId))
            call.respond(reminders.map { it.toDto() })
        }

        delete("/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")

            val success = service.deleteReminder(UUID.fromString(id))
            if (success) {
                call.respondText("Reminder deleted successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Reminder not found")
            }
        }
    }
}