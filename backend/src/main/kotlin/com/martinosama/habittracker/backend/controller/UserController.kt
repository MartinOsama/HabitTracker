package com.martinosama.habittracker.backend.controller

import com.martinosama.habittracker.backend.dto.UserDto
import com.martinosama.habittracker.backend.mapper.toDto
import com.martinosama.habittracker.backend.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {

    route("/user") {

        get("/{firebaseUid}") {
            val firebaseUid = call.parameters["firebaseUid"] ?: return@get call.respondText("Missing UID", status = HttpStatusCode.BadRequest)
            val user = userService.getUser(firebaseUid)
            if (user != null) {
                call.respond(user.toDto())
            } else {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/{firebaseUid}") {
            val firebaseUid = call.parameters["firebaseUid"] ?: return@post call.respondText("Missing UID", status = HttpStatusCode.BadRequest)
            val email = call.request.queryParameters["email"] ?: return@post call.respondText("Missing email", status = HttpStatusCode.BadRequest)
            val dto = call.receive<UserDto>()

            val existingUser = userService.getUser(firebaseUid)
            if (existingUser != null) {
                call.respond(existingUser.toDto())
            } else {
                val newUser = userService.createUser(firebaseUid, email, dto)
                call.respond(newUser.toDto())
            }
        }

        put("/{firebaseUid}") {
            val firebaseUid = call.parameters["firebaseUid"] ?: return@put call.respondText("Missing UID", status = HttpStatusCode.BadRequest)
            val dto = call.receive<UserDto>()
            val updated = userService.updateUser(firebaseUid, dto)
            if (updated) {
                call.respondText("User updated successfully")
            } else {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            }
        }
    }
}