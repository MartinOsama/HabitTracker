package com.martinosama.habittracker.backend.mapper

import com.martinosama.habittracker.backend.dto.HabitDto
import com.martinosama.habittracker.backend.entity.Habits
import com.martinosama.habittracker.backend.model.HabitModel
import org.jetbrains.exposed.sql.ResultRow
import java.time.format.DateTimeFormatter

fun ResultRow.toHabitModel() = HabitModel(
    id = this[Habits.id].value,
    userId = this[Habits.userId],
    title = this[Habits.title],
    notes = this[Habits.notes],
    date = this[Habits.date],
    fromTime = this[Habits.fromTime],
    toTime = this[Habits.toTime],
    reminder = this[Habits.reminder],
    isCompleted = this[Habits.isCompleted]
)

fun HabitModel.toDto(): HabitDto {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return HabitDto(
        id = this.id.toString(),
        title = this.title,
        notes = this.notes,
        date = this.date.format(dateFormatter),
        fromTime = this.fromTime.format(timeFormatter),
        toTime = this.toTime.format(timeFormatter),
        reminder = this.reminder,
        isCompleted = this.isCompleted
    )
}