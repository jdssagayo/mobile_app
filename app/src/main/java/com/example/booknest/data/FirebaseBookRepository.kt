package com.example.booknest.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseBookRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val booksCollection = db.collection("books")

    fun getAllBooks(): Flow<List<Book>> {
        return booksCollection.snapshots().map { snapshot ->
            snapshot.toObjects(Book::class.java)
        }
    }

    fun getFavoriteBooks(userId: String): Flow<List<Book>> {
        return booksCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isFavorite", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Book::class.java)
            }
    }

    fun getDraftBooks(userId: String): Flow<List<Book>> {
        return booksCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isDraft", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Book::class.java)
            }
    }

    suspend fun getBookById(bookId: String): Book? {
        return try {
            booksCollection.document(bookId).get().await().toObject(Book::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveBook(book: Book) {
        if (book.id.isBlank()) {
            booksCollection.add(book).await()
        } else {
            booksCollection.document(book.id).set(book).await()
        }
    }

    suspend fun deleteBook(bookId: String) {
        booksCollection.document(bookId).delete().await()
    }

    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        booksCollection.document(bookId).update("isFavorite", isFavorite).await()
    }
}
