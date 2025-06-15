package com.martinosama.habittracker.backend

import com.martinosama.habittracker.model.HabitDto
import retrofit2.Response
import retrofit2.http.*

interface HabitApiService {

    @GET("habits/{firebaseUid}")
    suspend fun getHabits(
        @Path("firebaseUid") uid: String
    ): List<HabitDto>

    @GET("habits/one/{habitId}")
    suspend fun getHabitById(
        @Path("habitId") habitId: String
    ): HabitDto

    @POST("habits/{firebaseUid}")
    suspend fun createHabit(
        @Path("firebaseUid") uid: String,
        @Body habit: HabitDto
    ): HabitDto

    @PUT("habits/{habitId}")
    suspend fun updateHabit(
        @Path("habitId") habitId: String,
        @Body habit: HabitDto
    ): Response<Unit>

    @DELETE("habits/{habitId}")
    suspend fun deleteHabit(
        @Path("habitId") habitId: String
    ): Response<Unit>
}