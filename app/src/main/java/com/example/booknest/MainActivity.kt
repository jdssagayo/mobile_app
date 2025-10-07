package com.example.booknest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booknest.ui.theme.BookNestTheme
import java.util.UUID

// --- Data Classes ---

enum class AppScreen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Saved("Saved", Icons.AutoMirrored.Filled.List),
    Drafts("Drafts", Icons.Filled.Edit),
    Profile("Profile", Icons.Filled.Person),
    Search("Search", Icons.Filled.Search),
    WritingOptions("New Draft", Icons.Filled.Add),
    BookDetail("Details", Icons.Filled.Info),
    Settings("Settings", Icons.Filled.Settings),
    AddNote("Add Note", Icons.Filled.Edit),
    AddReminder("Set Reminder", Icons.Filled.Edit)
}

data class Book(val id: String, val title: String, val author: String, val coverUrl: String, val genre: String, var isDownloaded: Boolean = false)

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookNestApp()
        }
    }
}

// --- Main App Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookNestApp() {
    val currentUserId = remember { UUID.randomUUID().toString() }
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var detailBookId by remember { mutableStateOf<String?>(null) }
    var darkTheme by remember { mutableStateOf(false) }

    val mockBooks = remember { mutableStateListOf(
        Book("1", "The Path to Quality Education", "A. Smith", "https://placehold.co/100x150/ff7f7f/ffffff?text=SDG+4", "Non-fiction"),
        Book("2", "Echoes of the Future", "B. Jones", "https://placehold.co/100x150/7f7fff/ffffff?text=Fiction", "Fantasy"),
        Book("3", "Writing Fundamentals", "C. Lee", "https://placehold.co/100x150/7fff7f/ffffff?text=Writing", "Education"),
        Book("4", "The Silent Reader", "D. Wu", "https://placehold.co/100x150/ffff7f/000000?text=Novel", "Mystery"),
    )}

    val navItems = listOf(AppScreen.Home, AppScreen.Saved, AppScreen.Drafts, AppScreen.Profile)

    val navigate: (AppScreen, String?) -> Unit = { screen, id ->
        detailBookId = id
        if (navItems.contains(screen)) {
            detailBookId = null
        }
        currentScreen = screen
    }

    fun onDownloadBook(bookId: String) {
        val book = mockBooks.find { it.id == bookId }?.let { 
            it.isDownloaded = !it.isDownloaded
        }
    }

    BookNestTheme(darkTheme = darkTheme) {
        Scaffold(
            topBar = { BookNestTopBar(currentScreen = currentScreen, onNavigate = navigate) },
            bottomBar = { BookNestBottomBar(currentScreen = currentScreen, navItems = navItems, onNavigate = navigate) },
            floatingActionButton = {
                if (currentScreen == AppScreen.Home || currentScreen == AppScreen.Saved || currentScreen == AppScreen.Drafts) {
                    FloatingActionButton(
                        onClick = { navigate(AppScreen.WritingOptions, null) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Start Writing")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    AppScreen.Home -> HomeScreen(onNavigate = navigate, mockBooks = mockBooks)
                    AppScreen.Saved -> SavedScreen(onNavigate = navigate, mockBooks = mockBooks)
                    AppScreen.Drafts -> DraftsScreen(onNavigate = navigate)
                    AppScreen.Profile -> ProfileScreen(onNavigate = navigate, userId = currentUserId, darkTheme = darkTheme, onThemeChange = { darkTheme = it })
                    AppScreen.Search -> SearchScreen(onNavigate = navigate)
                    AppScreen.WritingOptions -> WritingOptionsScreen(onNavigate = navigate)
                    AppScreen.Settings -> SettingsScreen(onNavigate = navigate, darkTheme = darkTheme, onThemeChange = { darkTheme = it })
                    AppScreen.AddNote -> AddNoteScreen(onNavigate = navigate, bookId = detailBookId)
                    AppScreen.AddReminder -> AddReminderScreen(onNavigate = navigate, bookId = detailBookId)
                    AppScreen.BookDetail -> BookDetailScreen(onNavigate = navigate, bookId = detailBookId, mockBooks = mockBooks, onDownloadBook = ::onDownloadBook)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookNestTopBar(currentScreen: AppScreen, onNavigate: (AppScreen, String?) -> Unit) {
    val isRootScreen = listOf(AppScreen.Home, AppScreen.Saved, AppScreen.Drafts, AppScreen.Profile).contains(currentScreen)

    TopAppBar(
        title = { Text(if (isRootScreen && currentScreen != AppScreen.Home) currentScreen.label else "BookNest", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        navigationIcon = {
            if (!isRootScreen) {
                val backDestination = when (currentScreen) {
                    AppScreen.Settings -> AppScreen.Profile
                    AppScreen.Search, AppScreen.WritingOptions -> AppScreen.Home
                    AppScreen.BookDetail -> AppScreen.Home
                    AppScreen.AddNote, AppScreen.AddReminder -> AppScreen.BookDetail
                    else -> AppScreen.Home
                }
                IconButton(onClick = { onNavigate(backDestination, null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (currentScreen == AppScreen.Home) {
                IconButton(onClick = { onNavigate(AppScreen.Search, null) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(onClick = { /* Handle Notifications */ }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun BookNestBottomBar(currentScreen: AppScreen, navItems: List<AppScreen>, onNavigate: (AppScreen, String?) -> Unit) {
    val isRootScreen = navItems.contains(currentScreen)

    if (isRootScreen) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            navItems.forEach { screen ->
                val isSelected = currentScreen == screen
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                    label = { Text(screen.label, fontSize = 10.sp) },
                    selected = isSelected,
                    onClick = { onNavigate(screen, null) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

@Composable
fun BookItem(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .padding(8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Image(
                imageVector = Icons.Filled.Info,
                contentDescription = "${book.title} cover",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigate: (AppScreen, String?) -> Unit, mockBooks: List<Book>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Text("Hi, what would you like to read today?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(16.dp))
            Text("Recommended for you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        if (mockBooks.isNotEmpty()) {
            items(mockBooks.chunked(2)) { rowBooks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowBooks.forEach { book ->
                        BookItem(book) { onNavigate(AppScreen.BookDetail, book.id) }
                    }
                    if (rowBooks.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("Stories of Quality Education", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            items(mockBooks.takeLast(2).chunked(2)) { rowBooks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowBooks.forEach { book ->
                        BookItem(book) { onNavigate(AppScreen.BookDetail, book.id) }
                    }
                    if (rowBooks.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No books available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
fun SavedScreen(onNavigate: (AppScreen, String?) -> Unit, mockBooks: List<Book>) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(top = 16.dp)) {
        Text(
            "Your Saved Reading List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        if (mockBooks.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(mockBooks) { book ->
                    ListItem(
                        headlineContent = { Text(book.title) },
                        supportingContent = { Text("by ${book.author} | ${book.genre}") },
                        leadingContent = {
                            if (book.isDownloaded) {
                                Icon(Icons.Filled.DownloadDone, contentDescription = "Downloaded")
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Book Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable { onNavigate(AppScreen.BookDetail, book.id) }
                            .padding(horizontal = 16.dp)
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved books found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DraftsScreen(onNavigate: (AppScreen, String?) -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(top = 16.dp)) {
        Text(
            "My Drafts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Drafts A",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        val drafts = listOf("A Personal Essay on SDG 4", "The Lost City Chapter 1", "Poem: The Learning Tree")

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(drafts) { draftTitle ->
                ListItem(
                    headlineContent = { Text(draftTitle) },
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = "Draft") },
                    trailingContent = {
                        IconButton(onClick = { /* TODO: Handle more options for this draft */ }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                    },
                    modifier = Modifier
                        .clickable {
                            onNavigate(AppScreen.WritingOptions, draftTitle) // Using draftTitle as a placeholder ID
                        }
                        .padding(horizontal = 16.dp)
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ProfileScreen(onNavigate: (AppScreen, String?) -> Unit, userId: String, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("B", fontSize = 40.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text("BookNest User", style = MaterialTheme.typography.headlineSmall)
        Text("User ID: $userId", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = darkTheme,
                onCheckedChange = onThemeChange
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(AppScreen.Settings, null) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Account and settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Manage your profile, data, and preferences.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        val menuItems = listOf(
            "Feedback" to Icons.AutoMirrored.Filled.Send,
            "About us" to Icons.Filled.Info,
            "Disclaimer" to Icons.Filled.Info
        )
        menuItems.forEach { (label, icon) ->
            ListItem(
                headlineContent = { Text(label) },
                leadingContent = { Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go") },
                modifier = Modifier.clickable { /* Navigate to specific screen */ }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun WritingOptionsScreen(onNavigate: (AppScreen, String?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Start a New Creation", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onNavigate(AppScreen.Drafts, null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Write an article/essay", fontSize = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onNavigate(AppScreen.Drafts, null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Write fiction", fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onNavigate: (AppScreen, String?) -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search for books, authors, or topics...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("Search Results will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(AppScreen.Home, null) }) { Text("Go Home") }
    }
}

@Composable
fun BookDetailScreen(onNavigate: (AppScreen, String?) -> Unit, bookId: String?, mockBooks: List<Book>, onDownloadBook: (String) -> Unit) {
    val currentBook = mockBooks.find { it.id == bookId }

    if (currentBook == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Book Not Found", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text("The requested book ID ($bookId) is invalid or missing.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onNavigate(AppScreen.Home, null) }) {
                Text("Return to Home")
            }
        }
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {
        Text("Book Detail", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Title: ${currentBook.title}", style = MaterialTheme.typography.titleLarge)
        Text("Author: ${currentBook.author}", style = MaterialTheme.typography.titleMedium)
        Text("Genre: ${currentBook.genre}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Image(
            imageVector = Icons.Filled.Info,
            contentDescription = "Cover of ${currentBook.title}",
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text("This is the screen where the user can read, add notes, and set reminders, as per your wireframe. (Content for ${currentBook.title})")
        Spacer(Modifier.height(24.dp))

        Button(onClick = { onDownloadBook(currentBook.id) }) {
            Icon(if (currentBook.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download, contentDescription = "Download")
            Spacer(Modifier.width(8.dp))
            Text(if (currentBook.isDownloaded) "Downloaded" else "Download")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = { onNavigate(AppScreen.AddNote, currentBook.id) }) {
            Icon(Icons.Filled.Edit, contentDescription = "Add Note")
            Spacer(Modifier.width(8.dp))
            Text("Add Note")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNavigate(AppScreen.AddReminder, currentBook.id) }) {
            Icon(Icons.Filled.Edit, contentDescription = "Set Reminder")
            Spacer(Modifier.width(8.dp))
            Text("Add Reading Reminder")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(onNavigate: (AppScreen, String?) -> Unit, bookId: String?) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Add New Note for Book ID: ${bookId ?: "Unknown"}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        var noteContent by remember { mutableStateOf("") }
        OutlinedTextField(
            value = noteContent,
            onValueChange = { noteContent = it },
            label = { Text("Your Note Content...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(AppScreen.BookDetail, bookId) }) {
            Text("Save Note")
        }
    }
}

@Composable
fun AddReminderScreen(onNavigate: (AppScreen, String?) -> Unit, bookId: String?) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Set Reading Reminder for Book ID: ${bookId ?: "Unknown"}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Reminder Date: (Tap to select date)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Reminder Time: (Tap to select time)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(AppScreen.BookDetail, bookId) }) {
            Text("Set Reminder")
        }
    }
}

@Composable
fun SettingsScreen(onNavigate: (AppScreen, String?) -> Unit, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Account, notifications, data, and general preferences.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        ListItem(
            headlineContent = { Text("Notification Preferences") },
            leadingContent = { Icon(Icons.Filled.Notifications, "Notifications")},
            modifier = Modifier.clickable { /* TODO */ }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Data Usage") },
            leadingContent = { Icon(Icons.Filled.Info, "Data Usage")},
            modifier = Modifier.clickable { /* TODO */ }
        )
        HorizontalDivider()
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onNavigate(AppScreen.Profile, null) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("Sign Out", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BookNestTheme {
        BookNestApp()
    }
}

