package com.example.booknest.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booknest.data.Chapter
import com.example.booknest.data.FirebaseChapterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChapterViewModel(private val repository: FirebaseChapterRepository) : ViewModel() {

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    fun getChaptersForBook(bookId: String) {
        viewModelScope.launch {
            repository.getChaptersForBook(bookId).collect {
                _chapters.value = it
            }
        }
    }

    fun saveChapter(chapter: Chapter) {
        viewModelScope.launch {
            repository.saveChapter(chapter)
            // After saving, refresh the chapter list for the current book.
            getChaptersForBook(chapter.bookId)
        }
    }

    fun deleteChapter(chapterId: String, bookId: String) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
            // After deleting, also refresh the chapter list.
            getChaptersForBook(bookId)
        }
    }
}
