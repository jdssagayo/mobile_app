package com.example.booknest.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.booknest.data.Book
import com.example.booknest.data.Chapter
import com.example.booknest.data.FirebaseBookRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WriteBookViewModel(
    private val repository: FirebaseBookRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _selectedChapter = MutableStateFlow<Chapter?>(null)
    val selectedChapter: StateFlow<Chapter?> = _selectedChapter.asStateFlow()

    private val _newChapterCreated = MutableStateFlow<Chapter?>(null)
    val newChapterCreated: StateFlow<Chapter?> = _newChapterCreated.asStateFlow()

    private val _wordCountError = MutableStateFlow(false)
    val wordCountError: StateFlow<Boolean> = _wordCountError.asStateFlow()

    private var currentBookId: String? = null

    fun loadDraft(bookId: String?) {
        if (bookId == null || bookId == "new") {
            val newBook = Book(isDraft = true, userId = userId)
            val firstChapter = Chapter(title = "Chapter 1", bookId = newBook.id, userId = userId)
            _book.value = newBook
            _chapters.value = listOf(firstChapter)
            _selectedChapter.value = firstChapter
            currentBookId = null
            return
        }

        currentBookId = bookId
        viewModelScope.launch {
            _book.value = repository.getDraftBook(userId, bookId)
            repository.getChapters(userId, bookId).collect {
                _chapters.value = it
                val currentSelected = _selectedChapter.value
                if (currentSelected == null || it.none { chapter -> chapter.id == currentSelected.id }) {
                    _selectedChapter.value = it.firstOrNull()
                }
            }
        }
    }

    fun selectChapter(chapterId: String) {
        _selectedChapter.value = _chapters.value.find { it.id == chapterId }
    }

    fun addNewChapterAndSelect() {
        val bookId = currentBookId ?: return // Cannot add to a book that has never been saved
        viewModelScope.launch {
            val newChapter = Chapter(
                title = "Chapter ${_chapters.value.size + 1}",
                bookId = bookId,
                userId = userId
            )
            val savedChapter = repository.saveChapter(userId, bookId, newChapter)
            _chapters.value = _chapters.value + savedChapter // Update the list
            _newChapterCreated.value = savedChapter // Fire event to trigger navigation
        }
    }

    fun onNewChapterHandled() {
        _newChapterCreated.value = null
    }

    suspend fun save(title: String, chapterContent: String): String? {
        val wordCount = chapterContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        if (wordCount > 300) {
            _wordCountError.value = true
            return null
        }
        _wordCountError.value = false

        val bookToSave = (_book.value ?: Book(isDraft = true, userId = userId)).copy(title = title)
        val savedBookId = repository.saveDraft(userId, bookToSave)
        currentBookId = savedBookId
        _book.value = bookToSave.copy(id = savedBookId)

        val chapterToSave = (_selectedChapter.value ?: Chapter()).copy(bookId = savedBookId, content = chapterContent, userId = userId)
        val savedChapter = repository.saveChapter(userId, savedBookId, chapterToSave)

        _selectedChapter.value = savedChapter
        val chapterList = _chapters.value.toMutableList()
        val index = chapterList.indexOfFirst { it.id == savedChapter.id || it.id.isBlank() }
        if (index != -1) {
            chapterList[index] = savedChapter
        } else {
            chapterList.add(savedChapter)
        }
        _chapters.value = chapterList

        return savedBookId
    }

    fun publish() {
        val bookToPublish = _book.value ?: return
        if (bookToPublish.id.isBlank()) return

        viewModelScope.launch {
            repository.publishBook(userId, bookToPublish)
        }
    }

    fun clearWordCountError() {
        _wordCountError.value = false
    }
}

class WriteBookViewModelFactory(private val repository: FirebaseBookRepository, private val auth: FirebaseAuth) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WriteBookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WriteBookViewModel(repository, auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
