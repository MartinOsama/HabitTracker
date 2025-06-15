package com.martinosama.habittracker.backend.model

import java.util.*

data class UserModel(
    val id: UUID,
    val firebaseUid: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val birthDate: String?,
    val gender: String?,
    val avatarFileId: String?
)