package com.example.booknest.data

import com.google.firebase.firestore.DocumentId

data class Chapter(
    @DocumentId val id: String = "",
    val title: String = "",
    val content: String = "",
    val bookId: String = "",
    val timestamp: Long = 0L
)
