package com.martinosama.habittracker.backend

import com.martinosama.habittracker.model.UserDto
import retrofit2.http.*

interface UserApiService {

    @GET("user/{firebaseUid}")
    suspend fun getUser(@Path("firebaseUid") uid: String): UserDto

    @POST("user/{firebaseUid}")
    suspend fun createUser(
        @Path("firebaseUid") uid: String,
        @Query("email") email: String,
        @Body userDto: UserDto
    ): UserDto

    @PUT("user/{firebaseUid}")
    suspend fun updateUser(
        @Path("firebaseUid") uid: String,
        @Body userDto: UserDto
    ): retrofit2.Response<Unit>
}