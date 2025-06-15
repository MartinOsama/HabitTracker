package com.martinosama.habittracker.backend.repository

import com.martinosama.habittracker.backend.entity.Reminders
import com.martinosama.habittracker.backend.mapper.toModel
import com.martinosama.habittracker.backend.model.ReminderModel
import com.martinosama.habittracker.backend.dto.ReminderDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ReminderRepository {

    fun createReminder(dto: ReminderDto): ReminderModel {
        val id = UUID.randomUUID()
        transaction {
            Reminders.insert {
                it[Reminders.id] = id
                it[habitId] = UUID.fromString(dto.habitId)
                it[remindAt] = dto.remindAt
            }
        }
        return dto.toModel(id)
    }

    fun getRemindersForHabit(habitId: UUID): List<ReminderModel> {
        return transaction {
            Reminders.select { Reminders.habitId eq habitId }
                .map {
                    ReminderModel(
                        id = it[Reminders.id],
                        habitId = it[Reminders.habitId],
                        remindAt = it[Reminders.remindAt]
                    )
                }
        }
    }

    fun deleteReminder(id: UUID): Boolean {
        return transaction {
            Reminders.deleteWhere { Reminders.id eq id } > 0
        }
    }
}