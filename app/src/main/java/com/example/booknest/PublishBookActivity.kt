package com.example.booknest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booknest.data.Book
import com.example.booknest.ui.theme.BookNestTheme
import com.example.booknest.viewModel.BookViewModel
import com.example.booknest.viewModel.BookViewModelFactory

class PublishBookActivity : ComponentActivity() {

    private val bookViewModel: BookViewModel by viewModels {
        val app = application as BookNestApp
        BookViewModelFactory(app.repository, app.auth)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getStringExtra("BOOK_ID") // Get bookId from Intent

        setContent {
            BookNestTheme(darkTheme = true) { // Force dark theme for this screen
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A012E) // Dark purple background
                ) {
                    PublishScreen(
                        bookId = bookId,
                        viewModel = bookViewModel,
                        onPublish = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    bookId: String?,
    viewModel: BookViewModel,
    onPublish: () -> Unit
) {
    var bookState by remember { mutableStateOf<Book?>(null) }
    var bookTitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Fiction") }
    var language by remember { mutableStateOf("English") }
    var visibility by remember { mutableStateOf("Public") }

    // Trigger the load
    LaunchedEffect(bookId) {
        if (bookId != null) {
            viewModel.loadBook(bookId)
        }
    }

    // Collect the state from the ViewModel
    val bookFromViewModel by viewModel.currentBook.collectAsState()

    // Update the UI's local state once the book is loaded from the ViewModel
    LaunchedEffect(bookFromViewModel) {
        bookFromViewModel?.let { book ->
            bookState = book
            bookTitle = book.title
            description = book.description
            genre = book.genre
            language = book.language
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Book Cover Section
        Text("Book Cover", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload cover image",
                        tint = Color(0xFF6200EE),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                            .padding(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Upload cover image", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("PNG, JPG up to 10MB", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Book Title Section
        Text("Book Title", color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = bookTitle,
            onValueChange = { bookTitle = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6200EE),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            )
        )

        Spacer(Modifier.height(16.dp))

        // Description Section
        Text("Description", color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Write a compelling description...", color = Color.Gray) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                 focusedBorderColor = Color(0xFF6200EE),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                 cursorColor = Color.White
            )
        )

        Spacer(Modifier.height(16.dp))

        // Genre and Language Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Genre", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenu(
                    options = listOf("Fiction", "Non-Fiction", "Fantasy", "Sci-Fi"),
                    selectedOption = genre,
                    onOptionSelected = { genre = it }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Language", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenu(
                    options = listOf("English", "Spanish", "French", "German"),
                    selectedOption = language,
                    onOptionSelected = { language = it }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Visibility Section
        Text("Visibility", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { visibility = "Public" }
                ) {
                    RadioButton(
                        selected = visibility == "Public",
                        onClick = { visibility = "Public" },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6200EE), unselectedColor = Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Public", color = Color.White)
                        Text("Anyone can read your book", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { visibility = "Private" }
                ) {
                    RadioButton(
                        selected = visibility == "Private",
                        onClick = { visibility = "Private" },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6200EE), unselectedColor = Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Private", color = Color.White)
                        Text("Only you can access", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Publish Button
        Button(
            onClick = {
                bookState?.let {
                    val updatedBook = it.copy(
                        title = bookTitle,
                        description = description,
                        genre = genre,
                        language = language,
                        visibility = visibility,
                        isDraft = false
                    )
                    viewModel.publishBook(updatedBook)
                    onPublish()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6200EE), Color(0xFFB71CDE))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Publish", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenu(options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6200EE),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                 cursorColor = Color.White,
                unfocusedTrailingIconColor = Color.White,
                focusedTrailingIconColor = Color.White
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
