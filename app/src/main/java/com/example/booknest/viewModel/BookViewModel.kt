package com.example.booknest.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booknest.data.Book
import com.example.booknest.data.Chapter
import com.example.booknest.data.FirebaseBookRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookViewModel(private val repository: FirebaseBookRepository, private val auth: FirebaseAuth) : ViewModel() {

    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    val allBooks: StateFlow<List<Book>> = _allBooks.asStateFlow()

    private val _favoriteBooks = MutableStateFlow<List<Book>>(emptyList())
    val favoriteBooks: StateFlow<List<Book>> = _favoriteBooks.asStateFlow()

    private val _draftBooks = MutableStateFlow<List<Book>>(emptyList())
    val draftBooks: StateFlow<List<Book>> = _draftBooks.asStateFlow()

    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentBookChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val currentBookChapters: StateFlow<List<Chapter>> = _currentBookChapters.asStateFlow()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        if (currentUserId != null) {
            initializeDataForCurrentUser()
        }
    }

    fun onUserLoggedIn() {
        initializeDataForCurrentUser()
    }

    private fun initializeDataForCurrentUser() {
        fetchAllBooks()
        fetchFavoriteBooks()
        fetchDraftBooks()
    }

    private fun fetchAllBooks() {
        viewModelScope.launch {
            repository.getAllBooks().collect {
                _allBooks.value = it
            }
        }
    }

    private fun fetchFavoriteBooks() {
        currentUserId?.let {
            viewModelScope.launch {
                repository.getFavoriteBooks(it).collect {
                    _favoriteBooks.value = it
                }
            }
        }
    }

    private fun fetchDraftBooks() {
        currentUserId?.let {
            viewModelScope.launch {
                repository.getDraftBooks(it).collect {
                    _draftBooks.value = it
                }
            }
        }
    }

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _currentBook.value = repository.getBookById(bookId)
            // Also fetch the chapters for the now-current book
            repository.getPublicChapters(bookId).collect {
                _currentBookChapters.value = it
            }
        }
    }

    suspend fun saveBook(book: Book): String? {
        val userId = currentUserId ?: return null
        val result = repository.saveBook(book.copy(userId = userId))
        fetchDraftBooks()
        return result
    }

    fun publishBook(book: Book) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            repository.publishBook(userId, book)
            fetchAllBooks()
            fetchDraftBooks()
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            fetchAllBooks()
            fetchDraftBooks()
            fetchFavoriteBooks()
        }
    }

    fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(bookId, isFavorite)
            fetchAllBooks()
            fetchFavoriteBooks()
        }
    }
}
