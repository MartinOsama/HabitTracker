package com.martinosama.habittracker.backend.entity

import org.jetbrains.exposed.sql.Table

object Reminders : Table("reminders") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val habitId = uuid("habit_id").references(Habits.id)
    val remindAt = varchar("remind_at", 20)

    override val primaryKey = PrimaryKey(id)
}