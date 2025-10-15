package com.example.booknest.data

import com.google.firebase.firestore.DocumentId

data class Book(
    @DocumentId val id: String = "",
    val title: String = "",
    val author: String = "", // Added back to resolve UI errors
    val description: String = "",
    val genre: String = "",
    val language: String = "",
    val visibility: String = "Public",
    val isFavorite: Boolean = false,
    val isDraft: Boolean = false,
    val userId: String = "",
    val timestamp: Long = 0L
)
