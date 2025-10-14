
package com.example.booknest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.booknest.data.FirebaseBookRepository
import com.example.booknest.ui.theme.BookNestTheme
import com.example.booknest.viewModel.WriteBookViewModel
import com.example.booknest.viewModel.WriteBookViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class WriteBookActivity : ComponentActivity() {
    private lateinit var viewModel: WriteBookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getStringExtra("BOOK_ID")
        val factory = WriteBookViewModelFactory(FirebaseBookRepository(), Firebase.auth)
        viewModel = ViewModelProvider(this, factory).get(WriteBookViewModel::class.java)

        // Load the draft immediately.
        viewModel.loadDraft(bookId)

        setContent {
            BookNestTheme {
                WriteScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun WriteScreen(viewModel: WriteBookViewModel, onBack: () -> Unit) {
    val book by viewModel.book.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val wordCountError by viewModel.wordCountError.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Local states for the text fields
    var bookTitle by remember { mutableStateOf("") }
    var chapterContent by remember { mutableStateOf("") }

    // When the book or selected chapter from the ViewModel changes, update the local states.
    LaunchedEffect(book) {
        book?.let {
            if (bookTitle != it.title) {
                bookTitle = it.title
            }
        }
    }
    LaunchedEffect(selectedChapter) {
        selectedChapter?.let {
            if (chapterContent != it.content) {
                chapterContent = it.content
            }
        }
    }

    // Show a toast when the word count error is triggered
    LaunchedEffect(wordCountError) {
        if (wordCountError) {
            Toast.makeText(context, "Chapter cannot exceed 300 words.", Toast.LENGTH_LONG).show()
            viewModel.clearWordCountError() // Reset the error after showing it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val savedBookId = viewModel.save(bookTitle, chapterContent)
                            if (savedBookId != null) {
                                Toast.makeText(context, "Draft saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Draft")
                    }
                    IconButton(onClick = {
                        viewModel.publish()
                        Toast.makeText(context, "Book published!", Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Text("Publish")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addNewChapter() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Chapter")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // Book Title TextField
            OutlinedTextField(
                value = bookTitle,
                onValueChange = { bookTitle = it },
                label = { Text("Book Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chapter Dropdown
            var chapterMenuExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { chapterMenuExpanded = true }) {
                    Text(selectedChapter?.title ?: "Select Chapter")
                    Icon(Icons.Default.MoreVert, contentDescription = "Chapter Menu")
                }
                DropdownMenu(
                    expanded = chapterMenuExpanded,
                    onDismissRequest = { chapterMenuExpanded = false }
                ) {
                    chapters.forEach { chapter ->
                        DropdownMenuItem(
                            text = { Text(chapter.title) },
                            onClick = {
                                viewModel.selectChapter(chapter.id)
                                chapterMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chapter Content TextField
            OutlinedTextField(
                value = chapterContent,
                onValueChange = { chapterContent = it },
                label = { Text("Start writing your chapter...") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                isError = wordCountError
            )
        }
    }
}
