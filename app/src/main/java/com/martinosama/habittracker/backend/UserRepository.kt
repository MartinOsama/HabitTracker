package com.martinosama.habittracker.backend

import com.martinosama.habittracker.model.UserDto

class UserRepository(private val api: UserApiService) {
    suspend fun getUser(firebaseUid: String): UserDto? {
        return try {
            api.getUser(firebaseUid)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createUser(firebaseUid: String, email: String, user: UserDto): UserDto {
        return api.createUser(firebaseUid, email, user)
    }

    suspend fun updateUser(firebaseUid: String, user: UserDto): Boolean {
        return api.updateUser(firebaseUid, user).isSuccessful
    }
}