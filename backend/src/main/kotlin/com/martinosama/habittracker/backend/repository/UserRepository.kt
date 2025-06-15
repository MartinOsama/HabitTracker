package com.martinosama.habittracker.backend.repository

import com.martinosama.habittracker.backend.dto.UserDto
import com.martinosama.habittracker.backend.entity.Users
import com.martinosama.habittracker.backend.mapper.toUserModel
import com.martinosama.habittracker.backend.model.UserModel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

    fun getUserByFirebaseUid(firebaseUid: String): UserModel? = transaction {
        Users.select { Users.firebaseUid eq firebaseUid }
            .map { it.toUserModel() }
            .singleOrNull()
    }

    fun updateUserProfile(firebaseUid: String, dto: UserDto): Boolean = transaction {
        val updateCount = Users.update({ Users.firebaseUid eq firebaseUid }) { row ->
            row[firstName] = dto.firstName.takeIf { it.isNotBlank() }
            row[lastName] = dto.lastName.takeIf { it.isNotBlank() }
            row[birthDate] = dto.birthday.takeIf { it.isNotBlank() }
            row[gender] = dto.gender.takeIf { it.isNotBlank() }
            row[avatarFileId] = dto.profileImageUrl
        }
        updateCount > 0
    }

    fun createUser(firebaseUid: String, email: String, dto: UserDto): UserModel = transaction {
        val insertStatement = Users.insert { row ->
            row[Users.firebaseUid] = firebaseUid
            row[Users.email] = email
            row[firstName] = dto.firstName.takeIf { it.isNotBlank() }
            row[lastName] = dto.lastName.takeIf { it.isNotBlank() }
            row[birthDate] = dto.birthday.takeIf { it.isNotBlank() }
            row[gender] = dto.gender.takeIf { it.isNotBlank() }
            row[avatarFileId] = dto.profileImageUrl
        }
        insertStatement.resultedValues?.firstOrNull()?.toUserModel()!!
    }
}