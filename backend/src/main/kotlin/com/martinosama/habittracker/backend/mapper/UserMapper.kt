package com.martinosama.habittracker.backend.mapper

import com.martinosama.habittracker.backend.dto.UserDto
import com.martinosama.habittracker.backend.entity.Users
import com.martinosama.habittracker.backend.model.UserModel
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toUserModel() = UserModel(
    id = this[Users.id].value,
    firebaseUid = this[Users.firebaseUid],
    email = this[Users.email],
    firstName = this[Users.firstName],
    lastName = this[Users.lastName],
    birthDate = this[Users.birthDate],
    gender = this[Users.gender],
    avatarFileId = this[Users.avatarFileId]
)

fun UserModel.toDto() = UserDto(
    firstName = this.firstName ?: "",
    lastName = this.lastName ?: "",
    birthday = this.birthDate ?: "",
    gender = this.gender ?: "",
    profileImageUrl = this.avatarFileId
)