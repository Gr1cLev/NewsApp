package com.example.newsapp.model

data class UserProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
) {
    fun fullName(): String = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
}
