package com.martinosama.habittracker.backend.model

import java.util.UUID

data class ReminderModel(
    val id: UUID,
    val habitId: UUID,
    val remindAt: String
)