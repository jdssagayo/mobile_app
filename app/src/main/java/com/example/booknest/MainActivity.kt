package com.example.booknest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.booknest.data.Book
import com.example.booknest.data.FirebaseBookRepository
import com.example.booknest.ui.theme.BookNestTheme
import com.example.booknest.viewModel.BookViewModel
import com.example.booknest.viewModel.BookViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// --- New Dark Color Scheme for Drafts Screen ---
private val DraftsColorScheme = darkColorScheme(
    background = Color(0xFF1A1A2E),
    onBackground = Color.White,
    surface = Color(0xFF1A1A2E),
    onSurface = Color.White,
    primary = Color.White,
    onPrimary = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFF2A2A4A) // A slightly lighter variant for dividers
)

class MainActivity : ComponentActivity() {

    private lateinit var bookViewModel: BookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = BookViewModelFactory(FirebaseBookRepository(), Firebase.auth)
        bookViewModel = ViewModelProvider(this, factory)[BookViewModel::class.java]

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val currentScreen = AppScreen.fromRoute(currentRoute)
            val context = LocalContext.current

            // This call is now in the correct Composable scope.
            val systemIsDark = isSystemInDarkTheme()
            var useDarkTheme by remember { mutableStateOf(systemIsDark) }

            BookNestTheme(darkTheme = useDarkTheme) {
                Scaffold(
                    topBar = {
                        if (currentScreen == AppScreen.Drafts) {
                            // We apply the custom theme only to the Drafts top bar
                            MaterialTheme(colorScheme = DraftsColorScheme) {
                                DraftsTopBar(onNewDraft = {
                                    val intent = Intent(context, ChapterListActivity::class.java)
                                    intent.putExtra("BOOK_ID", "new")
                                    context.startActivity(intent)
                                })
                            }
                        } else {
                            BookNestTopBar(currentScreen) { screen, _ -> navController.navigate(screen.route) }
                        }
                    },
                    bottomBar = { BookNestBottomBar(currentScreen) { screen, _ -> navController.navigate(screen.route) } },
                ) { paddingValues ->
                    NavHost(navController, startDestination = AppScreen.Home.route, modifier = Modifier.padding(paddingValues)) {
                        composable(AppScreen.Home.route) { HomeScreen({ s, a -> navController.navigate("${s.route}${a ?: ""}") }, bookViewModel.allBooks.collectAsState().value) }
                        composable(AppScreen.Saved.route) { SavedScreen({ s, a -> navController.navigate("${s.route}${a ?: ""}") }, bookViewModel.favoriteBooks.collectAsState().value) }
                        composable(AppScreen.Drafts.route) {
                            // The custom theme is applied only to the screen content
                            MaterialTheme(colorScheme = DraftsColorScheme) {
                                DraftsScreen(
                                    books = bookViewModel.draftBooks.collectAsState().value,
                                    onEdit = { bookId ->
                                        val intent = Intent(context, ChapterListActivity::class.java)
                                        intent.putExtra("BOOK_ID", bookId)
                                        context.startActivity(intent)
                                    },
                                    onDelete = { bookId -> bookViewModel.deleteBook(bookId) }
                                )
                            }
                        }
                        composable(AppScreen.Profile.route) {
                            ProfileScreen(
                                onNavigate = { s, a -> navController.navigate("${s.route}${a ?: ""}") },
                                userId = Firebase.auth.currentUser?.uid ?: "N/A",
                                darkTheme = useDarkTheme,
                                onThemeChange = { useDarkTheme = it }, // Connect the switch to the theme state
                                onLogout = { /* TODO */ }
                            )
                        }
                        composable(AppScreen.BookDetail.routeWithArgs) {
                            val bookId = it.arguments?.getString("bookId")
                            BookDetailScreen(
                                onNavigate = { s, a -> navController.navigate("${s.route}${a ?: ""}") },
                                bookId = bookId,
                                viewModel = bookViewModel,
                                onDownload = { /* TODO */ }
                            )
                        }
                        composable(AppScreen.Search.route) { SearchScreen { s, a -> navController.navigate("${s.route}${a ?: ""}") } }
                        composable(AppScreen.Settings.route) { SettingsScreen { s, a -> navController.navigate("${s.route}${a ?: ""}") } }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bookViewModel.onUserLoggedIn() // Refresh data on resume
    }
}

// --- Enums and Data Classes ---

enum class AppScreen(val route: String, val label: String, val icon: ImageVector, val routeWithArgs: String = route) {
    Home("home", "Home", Icons.Filled.Home),
    Saved("saved", "Saved", Icons.Filled.Bookmark),
    Drafts("drafts", "Drafts", Icons.Filled.Edit),
    Profile("profile", "Profile", Icons.Filled.Person),
    BookDetail("bookDetail", "Book Detail", Icons.Filled.Info, "bookDetail/{bookId}"),
    Search("search", "Search", Icons.Filled.Search),
    Settings("settings", "Settings", Icons.Filled.Settings);

    companion object {
        fun fromRoute(route: String?): AppScreen {
            return entries.find { it.route == route } ?: Home
        }
    }
}

// --- Composables ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftsTopBar(onNewDraft: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "My Drafts",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        },
        actions = {
            IconButton(onClick = onNewDraft) {
                Icon(Icons.Default.Add, contentDescription = "New Draft", modifier = Modifier.size(32.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookNestTopBar(currentScreen: AppScreen, onNavigate: (AppScreen, String?) -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(currentScreen.label, fontWeight = FontWeight.Bold) },
        actions = {
            if (currentScreen == AppScreen.Home) {
                IconButton(onClick = { onNavigate(AppScreen.Search, null) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(onClick = { /* TODO: Notification screen */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        }
    )
}

@Composable
fun BookNestBottomBar(currentScreen: AppScreen, onNavigate: (AppScreen, String?) -> Unit) {
    val navItems = listOf(AppScreen.Home, AppScreen.Saved, AppScreen.Drafts, AppScreen.Profile)
    NavigationBar {
        navItems.forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label, fontSize = 10.sp) },
                selected = isSelected,
                onClick = { onNavigate(screen, null) }
            )
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
                imageVector = Icons.Filled.Book,
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
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigate: (AppScreen, String?) -> Unit, books: List<Book>) {
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

        if (books.isNotEmpty()) {
            items(books.chunked(2)) { rowBooks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowBooks.forEach { book ->
                        BookItem(book) { onNavigate(AppScreen.BookDetail, "/${book.id}") }
                    }
                    if (rowBooks.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        } else {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No books available right now.")
                }
            }
        }

        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
fun SavedScreen(onNavigate: (AppScreen, String?) -> Unit, books: List<Book>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            "Your Saved Reading List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        if (books.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(books) { book ->
                    ListItem(
                        headlineContent = { Text(book.title) },
                        supportingContent = { Text("by ${book.author} | ${book.genre}") },
                        leadingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Book Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .clickable { onNavigate(AppScreen.BookDetail, "/${book.id}") }
                            .padding(horizontal = 16.dp)
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved books found.")
            }
        }
    }
}

@Composable
fun DraftsScreen(
    books: List<Book>,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            if (books.isNotEmpty()) {
                items(books) { book ->
                    DraftItem(book = book, onEdit = { onEdit(book.id) }, onDelete = { onDelete(book.id) })
                }
            } else {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No drafts found. Tap \"+\" to start a new one.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun DraftItem(book: Book, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = book.title.ifBlank { "Untitled Draft" },
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
    }
}

@Composable
fun ProfileScreen(onNavigate: (AppScreen, String?) -> Unit, userId: String, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit, onLogout: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
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
                .padding(vertical = 8.dp)
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

        Button(onClick = { Firebase.auth.signOut(); onLogout() }) {
            Text("Logout")
        }
    }
}

@Composable
fun BookDetailScreen(
    onNavigate: (AppScreen, String?) -> Unit,
    bookId: String?,
    viewModel: BookViewModel,
    onDownload: (Book) -> Unit
) {
    LaunchedEffect(bookId) {
        if (bookId != null) {
            viewModel.loadBook(bookId)
        }
    }

    val book by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentBookChapters.collectAsState()
    val currentBook = book

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
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState())) {
        Text("Book Detail", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))
        Spacer(Modifier.height(8.dp))
        Text("Title: ${currentBook.title}", style = MaterialTheme.typography.titleLarge)
        Text("Author: ${currentBook.author}", style = MaterialTheme.typography.titleMedium)
        Text("Genre: ${currentBook.genre}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Image(
            imageVector = Icons.Filled.Book,
            contentDescription = "Cover of ${currentBook.title}",
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text(chapters.joinToString(separator = "\n\n") { it.content })
        Spacer(Modifier.height(24.dp))

        Button(onClick = { viewModel.toggleFavorite(currentBook.id, !currentBook.isFavorite) }) {
            Icon(if (currentBook.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Favorite")
            Spacer(Modifier.width(8.dp))
            Text(if (currentBook.isFavorite) "Favorited" else "Favorite")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = { onDownload(currentBook) }) {
            Icon(Icons.Filled.Download, contentDescription = "Download")
            Spacer(Modifier.width(8.dp))
            Text("Download")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SearchScreen(onNavigate: (AppScreen, String?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
            Text("Search Results will appear here.")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(AppScreen.Home, null) }) { Text("Go Home") }
    }
}

@Composable
fun SettingsScreen(onNavigate: (AppScreen, String?) -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Account, notifications, data, and general preferences.")
        Spacer(Modifier.height(24.dp))

        ListItem(
            headlineContent = { Text("Notification Preferences") },
            leadingContent = { Icon(Icons.Default.Notifications, "Notifications")},
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
