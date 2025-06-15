package com.martinosama.habittracker.backend.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time

object Habits : UUIDTable() {
    val userId = uuid("user_id").references(Users.id)
    val title = varchar("title", 255)
    val notes = text("notes").nullable()
    val date = date("date")
    val fromTime = time("from_time")
    val toTime = time("to_time")
    val reminder = varchar("reminder", 50).nullable()
    val isCompleted = bool("is_completed").default(false)
}