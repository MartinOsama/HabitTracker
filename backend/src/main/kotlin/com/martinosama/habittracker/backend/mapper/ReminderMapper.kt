package com.martinosama.habittracker.backend.mapper

import com.martinosama.habittracker.backend.dto.ReminderDto
import com.martinosama.habittracker.backend.model.ReminderModel
import java.util.UUID

fun ReminderModel.toDto(): ReminderDto = ReminderDto(
    id = id.toString(),
    habitId = habitId.toString(),
    remindAt = remindAt
)

fun ReminderDto.toModel(id: UUID): ReminderModel = ReminderModel(
    id = id,
    habitId = UUID.fromString(habitId),
    remindAt = remindAt
)