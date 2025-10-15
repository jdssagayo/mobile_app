package com.example.booknest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.booknest.data.Chapter
import com.example.booknest.data.FirebaseBookRepository
import com.example.booknest.viewModel.WriteBookViewModel
import com.example.booknest.viewModel.WriteBookViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// --- Color Schemes based on Mockups ---
private val DarkGreenColorScheme = darkColorScheme(
    primary = Color(0xFF50D387), background = Color(0xFF0E1F14), surface = Color(0xFF0E1F14),
    onPrimary = Color.Black, onBackground = Color(0xFFE0E3E0), onSurface = Color(0xFFE0E3E0),
    surfaceVariant = Color(0xFF1A2B20), onSurfaceVariant = Color(0xFFBFC9BF)
)

private val LightGreenColorScheme = lightColorScheme(
    primary = Color(0xFF006D39), background = Color(0xFFF6FBF3), surface = Color(0xFFF6FBF3),
    onPrimary = Color.White, onBackground = Color(0xFF1A1C1A), onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDDE5DA), onSurfaceVariant = Color(0xFF424942)
)

class WriteBookActivity : ComponentActivity() {
    private lateinit var viewModel: WriteBookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getStringExtra("BOOK_ID")
        val chapterId = intent.getStringExtra("CHAPTER_ID")

        val factory = WriteBookViewModelFactory(FirebaseBookRepository(), Firebase.auth)
        viewModel = ViewModelProvider(this, factory)[WriteBookViewModel::class.java]

        viewModel.loadDraft(bookId)
        if (chapterId != null) {
            viewModel.selectChapter(chapterId)
        }

        setContent {
            val book by viewModel.book.collectAsState()
            val selectedChapter by viewModel.selectedChapter.collectAsState()

            var isDarkMode by remember { mutableStateOf(true) }
            val colorScheme = if (isDarkMode) DarkGreenColorScheme else LightGreenColorScheme

            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WriteScreen(
                        viewModel = viewModel,
                        bookTitle = book?.title ?: "Untitled Book",
                        chapter = selectedChapter,
                        onThemeToggle = { isDarkMode = !isDarkMode },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    viewModel: WriteBookViewModel,
    bookTitle: String,
    chapter: Chapter?,
    onThemeToggle: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var titleState by remember(bookTitle) { mutableStateOf(bookTitle) }
    var contentState by remember { mutableStateOf("") }

    // Use LaunchedEffect to safely update the local content state when the chapter changes
    LaunchedEffect(chapter) {
        contentState = chapter?.content ?: ""
    }

    val wordCount = remember(contentState) {
        contentState.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleState, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onThemeToggle) { Icon(Icons.Filled.InvertColors, "Toggle Theme") }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val savedBookId = viewModel.save(titleState, contentState)
                            if (savedBookId != null) {
                                Toast.makeText(context, "Draft saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Icon(Icons.Filled.Save, "Save") }
                    IconButton(onClick = { /* TODO: More options */ }) { Icon(Icons.Filled.MoreVert, "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(chapter?.title ?: "Chapter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Text("$wordCount words", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))

            BasicTextField(
                value = contentState,
                onValueChange = { contentState = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, lineHeight = 24.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (contentState.isEmpty()) {
                            Text("Begin your story... ✨", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
