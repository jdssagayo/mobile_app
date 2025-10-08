package com.example.booknest.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Book(
    @DocumentId val id: String = "",
    val title: String = "",
    val author: String = "",
    val content: String = "",
    val genre: String = "",
    val isFavorite: Boolean = false,
    val isDraft: Boolean = false,
    val userId: String = "",
    @ServerTimestamp val createdAt: Date? = null
)
