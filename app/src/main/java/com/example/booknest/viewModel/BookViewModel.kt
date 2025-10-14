package com.example.booknest.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booknest.data.Book
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

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        // Fetch initial data only if a user is already logged in (e.g., app restart).
        if (currentUserId != null) {
            initializeDataForCurrentUser()
        }
    }

    // This is the new public function to call after login.
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
        }
    }

    suspend fun saveBook(book: Book): String? {
        val userId = currentUserId ?: return null // Don't save if user is not logged in
        val result = repository.saveBook(book.copy(userId = userId))
        
        // After saving a book, ALWAYS refresh the drafts list.
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
