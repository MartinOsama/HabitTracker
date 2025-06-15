package com.martinosama.habittracker.backend.service

import com.martinosama.habittracker.backend.dto.ReminderDto
import com.martinosama.habittracker.backend.model.ReminderModel
import com.martinosama.habittracker.backend.repository.ReminderRepository
import java.util.*

class ReminderService(private val repo: ReminderRepository) {

    fun createReminder(dto: ReminderDto): ReminderModel {
        return repo.createReminder(dto)
    }

    fun getRemindersForHabit(habitId: UUID): List<ReminderModel> {
        return repo.getRemindersForHabit(habitId)
    }

    fun deleteReminder(id: UUID): Boolean {
        return repo.deleteReminder(id)
    }
}