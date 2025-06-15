package com.martinosama.habittracker.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class HabitDto(
    val id: String? = null,
    val title: String,
    val notes: String? = null,
    val date: String,
    val fromTime: String,
    val toTime: String,
    val reminder: String? = null,
    val isCompleted: Boolean = false
)