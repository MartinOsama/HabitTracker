package com.martinosama.habittracker.model

data class UserDto(
    val firstName: String,
    val lastName: String,
    val birthday: String,
    val gender: String,
    val profileImageUrl: String? = null
)