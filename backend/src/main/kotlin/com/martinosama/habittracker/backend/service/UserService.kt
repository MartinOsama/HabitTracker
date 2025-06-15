package com.martinosama.habittracker.backend.service

import com.martinosama.habittracker.backend.dto.UserDto
import com.martinosama.habittracker.backend.model.UserModel
import com.martinosama.habittracker.backend.repository.UserRepository

class UserService(private val userRepository: UserRepository) {

    fun getUser(firebaseUid: String): UserModel? {
        return userRepository.getUserByFirebaseUid(firebaseUid)
    }

    fun updateUser(firebaseUid: String, dto: UserDto): Boolean {
        return userRepository.updateUserProfile(firebaseUid, dto)
    }

    fun createUser(firebaseUid: String, email: String, dto: UserDto): UserModel {
        return userRepository.createUser(firebaseUid, email, dto)
    }
}