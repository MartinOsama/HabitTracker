package com.martinosama.habittracker.backend.service

import com.martinosama.habittracker.backend.dto.HabitDto
import com.martinosama.habittracker.backend.model.HabitModel
import com.martinosama.habittracker.backend.repository.HabitRepository
import java.util.*
import org.slf4j.LoggerFactory

class HabitService(private val habitRepository: HabitRepository) {
    private val logger = LoggerFactory.getLogger(HabitService::class.java)

    fun getHabitById(id: UUID): HabitModel? {
        logger.info("Fetching habit with ID: $id")
        return habitRepository.getHabitById(id).also {
            logger.info("Habit fetched: $it")
        }
    }

    fun getHabitsByUser(userId: String): List<HabitModel> {
        logger.info("Fetching habits for user: $userId")
        return habitRepository.getHabitsForUser(userId).also {
            logger.info("Habits fetched for user $userId: $it")
        }
    }

    fun createHabit(userId: String, dto: HabitDto): HabitModel {
        logger.info("Creating habit for user: $userId, dto: $dto")
        return habitRepository.createHabit(userId, dto).also {
            logger.info("Habit created: $it")
        }
    }

    fun updateHabit(id: UUID, dto: HabitDto): Boolean {
        logger.info("Updating habit with ID: $id, dto: $dto")
        return habitRepository.updateHabit(id, dto).also {
            logger.info("Habit update result: $it")
        }
    }

    fun deleteHabit(id: UUID): Boolean {
        logger.info("Deleting habit with ID: $id")
        return habitRepository.deleteHabit(id).also {
            logger.info("Habit deletion result: $id")
        }
    }
}