package com.martinosama.habittracker.backend.controller

import com.martinosama.habittracker.backend.dto.HabitDto
import com.martinosama.habittracker.backend.mapper.toDto
import com.martinosama.habittracker.backend.respondException
import com.martinosama.habittracker.backend.service.HabitService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.habitRoutes(habitService: HabitService) {

    route("/habits") {

        get("/{firebaseUid}") {
            val uid = call.parameters["firebaseUid"]
            if (uid == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing UID")
                return@get
            }

            try {
                val habits = habitService.getHabitsByUser(uid)
                call.respond(habits.map { it.toDto() })
            } catch (e: Exception) {
                call.respondException(e)
            }
        }

        get("/one/{habitId}") {
            val habitIdParam = call.parameters["habitId"]
            if (habitIdParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing habit ID")
                return@get
            }

            val habitId = try {
                UUID.fromString(habitIdParam)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid UUID format")
                return@get
            }

            val habit = habitService.getHabitById(habitId)
            if (habit != null) {
                call.respond(habit.toDto())
            } else {
                call.respond(HttpStatusCode.NotFound, "Habit not found")
            }
        }

        post("/{firebaseUid}") {
            val uid = call.parameters["firebaseUid"]
            if (uid == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing UID")
                return@post
            }

            val dto = call.receive<HabitDto>().copy(id = UUID.randomUUID().toString())
            val createdHabit = habitService.createHabit(uid, dto)
            call.respond(HttpStatusCode.Created, createdHabit)
        }

        put("/{habitId}") {
            val habitIdParam = call.parameters["habitId"]
            if (habitIdParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing habit ID")
                return@put
            }

            val habitId = try {
                UUID.fromString(habitIdParam)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid UUID format")
                return@put
            }

            val dto = call.receive<HabitDto>()
            val updated = habitService.updateHabit(habitId, dto)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Habit updated")
            } else {
                call.respond(HttpStatusCode.NotFound, "Habit not found")
            }
        }

        delete("/{habitId}") {
            val habitIdParam = call.parameters["habitId"]
            if (habitIdParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing habit ID")
                return@delete
            }

            val habitId = try {
                UUID.fromString(habitIdParam)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid UUID format")
                return@delete
            }

            val deleted = habitService.deleteHabit(habitId)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Habit deleted")
            } else {
                call.respond(HttpStatusCode.NotFound, "Habit not found")
            }
        }
    }
}