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

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
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

    fun getBookById(bookId: String): StateFlow<Book?> {
        val flow = MutableStateFlow<Book?>(null)
        viewModelScope.launch {
            flow.value = repository.getBookById(bookId)
        }
        return flow
    }

    fun saveBook(book: Book) {
        viewModelScope.launch {
            currentUserId?.let {
                repository.saveBook(book.copy(userId = it))
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
        }
    }

    fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(bookId, isFavorite)
        }
    }
}
