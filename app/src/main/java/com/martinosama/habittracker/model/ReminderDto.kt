package com.martinosama.habittracker.model

data class ReminderDto(
    val id: String = "",
    val habitId: String,
    val remindAt: String
)