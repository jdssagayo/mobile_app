package com.example.booknest

import android.app.Application
import com.example.booknest.data.FirebaseBookRepository
import com.example.booknest.data.FirebaseChapterRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class BookNestApp : Application() {

    val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    val repository: FirebaseBookRepository by lazy {
        FirebaseBookRepository()
    }

    val chapterRepository: FirebaseChapterRepository by lazy {
        FirebaseChapterRepository()
    }
}
