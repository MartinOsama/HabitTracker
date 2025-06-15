package com.martinosama.habittracker.backend.dto

data class ReminderDto(
    val id: String,
    val habitId: String,
    val remindAt: String
)