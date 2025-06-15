package com.martinosama.habittracker.backend.repository

import com.martinosama.habittracker.backend.dto.HabitDto
import com.martinosama.habittracker.backend.entity.Habits
import com.martinosama.habittracker.backend.mapper.toHabitModel
import com.martinosama.habittracker.backend.model.HabitModel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class HabitRepository {
    private val logger = LoggerFactory.getLogger(HabitRepository::class.java)

    fun getHabitsForUser(userId: String): List<HabitModel> = transaction {
        logger.info("Querying habits for user: $userId")
        try {
            val result = Habits.select { Habits.userId eq UUID.fromString(userId) }
                .orderBy(Habits.date to SortOrder.ASC, Habits.fromTime to SortOrder.ASC)
                .map { it.toHabitModel() }
            logger.info("Query result for user $userId: $result")
            result
        } catch (e: Exception) {
            logger.error("Error querying habits for user $userId: ${e.message}", e)
            throw e
        }
    }

    fun getHabitById(id: UUID): HabitModel? = transaction {
        logger.info("Querying habit with ID: $id")
        try {
            val result = Habits.select { Habits.id eq id }
                .map { it.toHabitModel() }
                .singleOrNull()
            logger.info("Query result for habit $id: $result")
            result
        } catch (e: Exception) {
            logger.error("Error querying habit with ID $id: ${e.message}", e)
            throw e
        }
    }

    fun createHabit(userId: String, dto: HabitDto): HabitModel = transaction {
        logger.info("Inserting habit for user: $userId, dto: $dto")
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val insert = Habits.insert {
                it[Habits.userId] = UUID.fromString(userId)
                it[title] = dto.title
                it[notes] = dto.notes
                it[date] = LocalDate.parse(dto.date, dateFormatter)
                it[fromTime] = LocalTime.parse(dto.fromTime, timeFormatter)
                it[toTime] = LocalTime.parse(dto.toTime, timeFormatter)
                it[reminder] = dto.reminder
                it[isCompleted] = dto.isCompleted
            }
            val result = insert.resultedValues?.firstOrNull()?.toHabitModel()!!
            logger.info("Inserted habit: $result")
            result
        } catch (e: Exception) {
            logger.error("Error inserting habit for user $userId: ${e.message}", e)
            throw e
        }
    }

    fun updateHabit(id: UUID, dto: HabitDto): Boolean = transaction {
        logger.info("Updating habit with ID: $id, dto: $dto")
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val updated = Habits.update({ Habits.id eq id }) {
                it[title] = dto.title
                it[notes] = dto.notes
                it[date] = LocalDate.parse(dto.date, dateFormatter)
                it[fromTime] = LocalTime.parse(dto.fromTime, timeFormatter)
                it[toTime] = LocalTime.parse(dto.toTime, timeFormatter)
                it[reminder] = dto.reminder
                it[isCompleted] = dto.isCompleted
            }
            logger.info("Update result for habit $id: $updated")
            updated > 0
        } catch (e: Exception) {
            logger.error("Error updating habit with ID $id: ${e.message}", e)
            throw e
        }
    }

    fun deleteHabit(id: UUID): Boolean = transaction {
        logger.info("Deleting habit with ID: $id")
        try {
            val deleted = Habits.deleteWhere { Habits.id eq id }
            logger.info("Delete result for habit $id: $deleted")
            deleted > 0
        } catch (e: Exception) {
            logger.error("Error deleting habit with ID $id: ${e.message}", e)
            throw e
        }
    }
}