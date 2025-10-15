package com.example.booknest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.booknest.data.Chapter
import com.example.booknest.data.FirebaseBookRepository
import com.example.booknest.viewModel.WriteBookViewModel
import com.example.booknest.viewModel.WriteBookViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private val DarkGreenColorScheme = darkColorScheme(
    primary = Color(0xFF50D387), background = Color(0xFF0E1F14), surface = Color(0xFF0E1F14),
    onPrimary = Color.Black, onBackground = Color(0xFFE0E3E0), onSurface = Color(0xFFE0E3E0),
    surfaceVariant = Color(0xFF1A2B20), onSurfaceVariant = Color(0xFFBFC9BF)
)

class ChapterListActivity : ComponentActivity() {
    private lateinit var viewModel: WriteBookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bookId = intent.getStringExtra("BOOK_ID")

        val factory = WriteBookViewModelFactory(FirebaseBookRepository(), Firebase.auth)
        viewModel = ViewModelProvider(this, factory)[WriteBookViewModel::class.java]

        if (bookId != null) {
            viewModel.loadDraft(bookId)
        }

        setContent {
            // Use MaterialTheme directly to apply the custom color scheme
            MaterialTheme(colorScheme = DarkGreenColorScheme) {
                Surface {
                    ChapterListScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDraft(intent.getStringExtra("BOOK_ID"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(viewModel: WriteBookViewModel) {
    val context = LocalContext.current
    val book by viewModel.book.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val newChapter by viewModel.newChapterCreated.collectAsState()

    val openEditor = { chapterId: String ->
        val intent = Intent(context, WriteBookActivity::class.java).apply {
            putExtra("BOOK_ID", book?.id)
            putExtra("CHAPTER_ID", chapterId)
        }
        context.startActivity(intent)
    }

    // When a new chapter is created in the ViewModel, open it immediately
    LaunchedEffect(newChapter) {
        newChapter?.let {
            openEditor(it.id)
            viewModel.onNewChapterHandled() // Reset the event
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(book?.title ?: "Loading...", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { (context as? ChapterListActivity)?.finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.MoreVert, "Options") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addNewChapterAndSelect() }) {
                Icon(Icons.Default.Add, "Add Chapter")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            items(chapters) { chapter ->
                ChapterCard(chapter = chapter, onClick = { openEditor(chapter.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ChapterCard(chapter: Chapter, onClick: () -> Unit) {
    val wordCount = remember(chapter.content) {
        chapter.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = chapter.title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = chapter.content.ifEmpty { "No content yet..." },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "$wordCount words",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
