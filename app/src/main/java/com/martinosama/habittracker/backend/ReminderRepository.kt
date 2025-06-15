package com.martinosama.habittracker.backend

import com.martinosama.habittracker.model.ReminderDto

class ReminderRepository(private val api: ReminderApiService) {
    suspend fun createReminder(dto: ReminderDto): ReminderDto = api.createReminder(dto)
    suspend fun getReminders(habitId: String): List<ReminderDto> = api.getRemindersForHabit(habitId)
    suspend fun deleteReminder(id: String): Boolean = api.deleteReminder(id).isSuccessful
}