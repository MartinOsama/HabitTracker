package com.martinosama.habittracker.backend

import com.martinosama.habittracker.model.HabitDto

class HabitRepository(private val api: HabitApiService) {

    suspend fun getHabits(uid: String): List<HabitDto> {
        return api.getHabits(uid)
    }

    suspend fun getHabitById(id: String): HabitDto {
        return api.getHabitById(id)
    }

    suspend fun createHabit(uid: String, habit: HabitDto): HabitDto {
        return api.createHabit(uid, habit)
    }

    suspend fun updateHabit(id: String, habit: HabitDto): Boolean {
        return api.updateHabit(id, habit).isSuccessful
    }

    suspend fun deleteHabit(id: String): Boolean {
        return api.deleteHabit(id).isSuccessful
    }
}