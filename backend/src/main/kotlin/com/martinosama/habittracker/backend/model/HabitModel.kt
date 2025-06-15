package com.martinosama.habittracker.backend.model

import kotlinx.serialization.Contextual
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlinx.serialization.Serializable

@Serializable
data class HabitModel(
    @Contextual val id: UUID,
    @Contextual val userId: UUID,
    val title: String,
    val notes: String?,
    @Contextual val date: LocalDate,
    @Contextual val fromTime: LocalTime,
    @Contextual val toTime: LocalTime,
    val reminder: String?,
    val isCompleted: Boolean
)