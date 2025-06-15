package com.martinosama.habittracker.backend

import com.martinosama.habittracker.model.ReminderDto
import retrofit2.Response
import retrofit2.http.*

interface ReminderApiService {
    @POST("reminder")
    suspend fun createReminder(@Body dto: ReminderDto): ReminderDto

    @GET("reminder/{habitId}")
    suspend fun getRemindersForHabit(@Path("habitId") habitId: String): List<ReminderDto>

    @DELETE("reminder/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>
}