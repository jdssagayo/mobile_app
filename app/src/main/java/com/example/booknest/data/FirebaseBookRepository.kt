package com.example.booknest.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseBookRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    private val publicBooksCollection = db.collection("books")
    private fun userDraftsCollection(userId: String) = db.collection("users").document(userId).collection("drafts")
    private fun userFavoritesCollection(userId: String) = db.collection("users").document(userId).collection("favorites")
    private fun draftChaptersCollection(userId: String, bookId: String) = userDraftsCollection(userId).document(bookId).collection("chapters")
    private fun publicChaptersCollection(bookId: String) = publicBooksCollection.document(bookId).collection("chapters")

    private fun mapToBookList(snapshot: QuerySnapshot): List<Book> {
        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(Book::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                println("Error parsing book document ${doc.id}: $e")
                null
            }
        }
    }

    // --- Public, Published Books --- //

    fun getAllBooks(): Flow<List<Book>> {
        return publicBooksCollection
            .whereEqualTo("isDraft", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map(::mapToBookList)
    }

    suspend fun getBookById(bookId: String): Book? {
        return try {
            publicBooksCollection.document(bookId).get().await().toObject(Book::class.java)?.copy(id = bookId)
        } catch (e: Exception) {
            println("Error getting book $bookId: $e")
            null
        }
    }

    fun getPublicChapters(bookId: String): Flow<List<Chapter>> {
        return publicChaptersCollection(bookId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Chapter::class.java)?.copy(id = doc.id)
                }
            }
    }

    // --- User-Specific Drafts (for WriteBookViewModel) --- //

    fun getDraftBooks(userId: String): Flow<List<Book>> {
        return userDraftsCollection(userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map(::mapToBookList)
    }

    suspend fun getDraftBook(userId: String, bookId: String): Book? {
        return try {
            userDraftsCollection(userId).document(bookId).get().await().toObject(Book::class.java)?.copy(id = bookId)
        } catch (e: Exception) {
            println("Error getting draft book $bookId: $e")
            null
        }
    }

    suspend fun saveDraft(userId: String, book: Book): String {
        val docRef = if (book.id.isBlank()) {
            userDraftsCollection(userId).add(book.copy(timestamp = System.currentTimeMillis())).await()
        } else {
            userDraftsCollection(userId).document(book.id).set(book.copy(timestamp = System.currentTimeMillis())).await()
            userDraftsCollection(userId).document(book.id)
        }
        return docRef.id
    }

    suspend fun publishBook(userId: String, book: Book) {
        val publicBook = book.copy(isDraft = false, userId = userId, timestamp = System.currentTimeMillis())
        val publicBookRef = publicBooksCollection.document(book.id).apply {
            set(publicBook).await()
        }

        val draftChapters = draftChaptersCollection(userId, book.id).get().await()
        for (chapterDoc in draftChapters.documents) {
            val chapter = chapterDoc.toObject(Chapter::class.java)
            if (chapter != null) {
                publicChaptersCollection(publicBookRef.id).document(chapterDoc.id).set(chapter.copy(userId = userId)).await()
            }
        }

        draftChapters.documents.forEach { it.reference.delete().await() }
        userDraftsCollection(userId).document(book.id).delete().await()
    }

    // --- User-Specific Favorites --- //

    fun getFavoriteBooks(userId: String): Flow<List<Book>> {
        return userFavoritesCollection(userId).snapshots().flatMapLatest { favoriteIdsSnapshot ->
            val favoriteBookIds = favoriteIdsSnapshot.documents.map { it.id }
            if (favoriteBookIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                publicBooksCollection.whereIn(FieldPath.documentId(), favoriteBookIds)
                    .snapshots()
                    .map(::mapToBookList)
            }
        }
    }

    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val favoriteDoc = userFavoritesCollection(userId).document(bookId)
        if (isFavorite) {
            favoriteDoc.set(mapOf("favoritedAt" to System.currentTimeMillis())).await()
        } else {
            favoriteDoc.delete().await()
        }
    }

    // --- Generic Save/Delete (for BookViewModel) --- //

    suspend fun saveBook(book: Book): String {
        val userId = book.userId.ifBlank { auth.currentUser?.uid ?: error("User not logged in") }
        val bookWithUser = book.copy(userId = userId, timestamp = System.currentTimeMillis())

        return if (bookWithUser.isDraft) {
            saveDraft(userId, bookWithUser)
        } else {
            val publicBookRef = if (bookWithUser.id.isBlank()) {
                publicBooksCollection.add(bookWithUser).await()
            } else {
                publicBooksCollection.document(bookWithUser.id).set(bookWithUser).await()
                publicBooksCollection.document(bookWithUser.id)
            }
            publicBookRef.id
        }
    }

    suspend fun deleteBook(bookId: String) {
        val userId = auth.currentUser?.uid ?: return

        val draftRef = userDraftsCollection(userId).document(bookId)
        if (draftRef.get().await().exists()) {
            draftChaptersCollection(userId, bookId).get().await().documents.forEach { it.reference.delete().await() }
            draftRef.delete().await()
            return
        }

        val publicBookRef = publicBooksCollection.document(bookId)
        val publicBook = publicBookRef.get().await().toObject(Book::class.java)
        if (publicBook != null && publicBook.userId == userId) {
            publicChaptersCollection(bookId).get().await().documents.forEach { it.reference.delete().await() }
            publicBookRef.delete().await()
        }
    }

    // --- Chapters --- //

    fun getChapters(userId: String, bookId: String): Flow<List<Chapter>> {
        return draftChaptersCollection(userId, bookId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Chapter::class.java)?.copy(id = doc.id)
                }
            }
    }

    suspend fun saveChapter(userId: String, bookId: String, chapter: Chapter): Chapter {
        val chapterWithUser = chapter.copy(userId = userId, timestamp = System.currentTimeMillis())
        return if (chapter.id.isBlank()) {
            val newDocRef = draftChaptersCollection(userId, bookId).add(chapterWithUser).await()
            chapterWithUser.copy(id = newDocRef.id)
        } else {
            draftChaptersCollection(userId, bookId).document(chapter.id).set(chapterWithUser).await()
            chapterWithUser
        }
    }

    suspend fun deleteChapter(userId: String, bookId: String, chapterId: String) {
        draftChaptersCollection(userId, bookId).document(chapterId).delete().await()
    }
}
