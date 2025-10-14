package com.example.booknest.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseChapterRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val chaptersCollection = db.collection("chapters")

    fun getChaptersForBook(bookId: String): Flow<List<Chapter>> {
        return chaptersCollection
            .whereEqualTo("bookId", bookId)
            // .orderBy("timestamp") // This requires a manual index in Firestore. Removing it to prevent crashes.
            .snapshots()
            .map { snapshot ->
                val chapters = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Chapter::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        println("Error parsing chapter document ${doc.id}: $e")
                        null
                    }
                }
                // Sorting is now done on the client-side to avoid needing a composite index.
                chapters.sortedBy { it.timestamp }
            }
    }

    suspend fun saveChapter(chapter: Chapter) {
        if (chapter.id.isBlank()) {
            chaptersCollection.add(chapter.copy(timestamp = System.currentTimeMillis())).await()
        } else {
            chaptersCollection.document(chapter.id).set(chapter.copy(timestamp = System.currentTimeMillis())).await()
        }
    }

    suspend fun deleteChapter(chapterId: String) {
        chaptersCollection.document(chapterId).delete().await()
    }
}
