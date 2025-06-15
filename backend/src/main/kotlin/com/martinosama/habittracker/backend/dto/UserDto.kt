package com.martinosama.habittracker.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val firstName: String,
    val lastName: String,
    val birthday: String,
    val gender: String,
    val profileImageUrl: String? = null
)