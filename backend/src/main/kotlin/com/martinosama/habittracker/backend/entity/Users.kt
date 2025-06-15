package com.martinosama.habittracker.backend.entity

import org.jetbrains.exposed.dao.id.UUIDTable

object Users : UUIDTable() {
    val firebaseUid = varchar("firebase_uid", 128).uniqueIndex()
    val email = varchar("email", 255)
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val birthDate = varchar("birth_date", 20).nullable()
    val gender = varchar("gender", 10).nullable()
    val avatarFileId = varchar("avatar_file_id", 255).nullable()
}