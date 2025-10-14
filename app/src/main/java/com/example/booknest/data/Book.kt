package com.example.booknest.data

import com.google.firebase.firestore.DocumentId

data class Book(
    @DocumentId val id: String = "",
    val title: String = "",
    // val author: String = "",  // This was commented out in a previous version, keeping it that way.
    val description: String = "",
    val genre: String = "",
    val language: String = "",
    val visibility: String = "Public",
    val isFavorite: Boolean = false,
    val isDraft: Boolean = false,
    val userId: String = "",
    val timestamp: Long = 0L
    // The `content` field has been removed to avoid state-sync issues.
    // Content now lives exclusively in the `Chapter` objects.
)
