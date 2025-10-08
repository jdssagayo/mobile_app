package com.example.booknest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booknest.data.Book
import com.example.booknest.ui.theme.BookNestTheme
import com.example.booknest.viewModel.BookViewModel
import com.example.booknest.viewModel.BookViewModelFactory

class WriteBookActivity : ComponentActivity() {

    private val bookViewModel: BookViewModel by viewModels {
        val app = application as BookNestApp
        BookViewModelFactory(app.repository, app.auth)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getStringExtra("BOOK_ID")

        setContent {
            BookNestTheme {
                WriteBookScreen(
                    bookId = bookId,
                    viewModel = bookViewModel,
                    onSave = { finish() },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteBookScreen(
    bookId: String?,
    viewModel: BookViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(bookId) {
        if (bookId != null) {
            viewModel.getBookById(bookId).collect { book ->
                if (book != null) {
                    title = book.title
                    content = book.content
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val book = Book(
                                id = bookId ?: "",
                                title = title.ifBlank { "Untitled" },
                                author = "", // No longer in UI
                                genre = "", // No longer in UI
                                content = content,
                                isDraft = true // Always a draft from this screen
                            )
                            viewModel.saveBook(book)
                            onSave()
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { context.startActivity(Intent(context, ReminderActivity::class.java)) }) {
                    Icon(Icons.Default.Star, contentDescription = "Add Reminder", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Reminder")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { context.startActivity(Intent(context, NoteActivity::class.java)) }) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Add Note", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Note")
                }
            }
            HorizontalDivider()

            // Title Field
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                decorationBox = {
                    if (title.isEmpty()) {
                        Text("Add Title", style = TextStyle(fontSize = 24.sp, color = Color.Gray))
                    }
                    it()
                }
            )

            // Content Field
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                decorationBox = {
                    if (content.isEmpty()) {
                        Text("Start Writing...", style = TextStyle(fontSize = 16.sp, color = Color.Gray))
                    }
                    it()
                }
            )
        }
    }
}
