package com.example.booknest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.example.booknest.data.Book
import com.example.booknest.ui.theme.BookNestTheme
import com.example.booknest.viewModel.BookViewModel
import com.example.booknest.viewModel.BookViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
}

class MainActivity : ComponentActivity() {

    private val bookViewModel: BookViewModel by viewModels {
        val app = application as BookNestApp
        BookViewModelFactory(app.repository, app.auth)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val startDestination = if (Firebase.auth.currentUser != null) Routes.MAIN else Routes.LOGIN

            NavHost(navController = navController, startDestination = startDestination) {
                composable(Routes.LOGIN) {
                    LoginScreen(onLoginSuccess = {
                        bookViewModel.onUserLoggedIn()
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    })
                }
                composable(Routes.MAIN) {
                    BookNestApp(bookViewModel, onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun BookNestApp(viewModel: BookViewModel, onLogout: () -> Unit) {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var detailBookId by remember { mutableStateOf<String?>(null) }
    var darkTheme by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val allBooks by viewModel.allBooks.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val draftBooks by viewModel.draftBooks.collectAsState()

    val navItems = listOf(AppScreen.Home, AppScreen.Saved, AppScreen.Drafts, AppScreen.Profile)

    val navigate: (AppScreen, String?) -> Unit = { screen, id ->
        detailBookId = id
        if (navItems.contains(screen)) {
            detailBookId = null
        }
        currentScreen = screen
    }

    BookNestTheme(darkTheme = darkTheme) {
        Scaffold(
            topBar = { BookNestTopBar(currentScreen = currentScreen, onNavigate = navigate) },
            bottomBar = { BookNestBottomBar(currentScreen = currentScreen, navItems = navItems, onNavigate = navigate) },
            floatingActionButton = {
                if (currentScreen == AppScreen.Drafts) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, WriteBookActivity::class.java)
                            context.startActivity(intent)
                        },
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
                    AppScreen.Home -> HomeScreen(onNavigate = navigate, books = allBooks)
                    AppScreen.Saved -> SavedScreen(onNavigate = navigate, books = favoriteBooks)
                    AppScreen.Drafts -> DraftsScreen(onNavigate = navigate, books = draftBooks, onEdit = { bookId: String ->
                        val intent = Intent(context, WriteBookActivity::class.java)
                        intent.putExtra("BOOK_ID", bookId)
                        context.startActivity(intent)
                    }, onDelete = { bookId: String -> viewModel.deleteBook(bookId) })
                    AppScreen.Profile -> ProfileScreen(
                        onNavigate = navigate,
                        userId = Firebase.auth.currentUser?.uid ?: "",
                        darkTheme = darkTheme,
                        onThemeChange = { darkTheme = it },
                        onLogout = onLogout
                    )
                    AppScreen.Search -> SearchScreen(onNavigate = navigate)
                    AppScreen.BookDetail -> BookDetailScreen(
                        onNavigate = navigate,
                        bookId = detailBookId,
                        viewModel = viewModel,
                        onDownload = {
                            Toast.makeText(context, "Book downloaded for offline use", Toast.LENGTH_SHORT).show()
                        }
                    )
                    AppScreen.Settings -> SettingsScreen(onNavigate = navigate)
                    else -> HomeScreen(onNavigate = navigate, books = allBooks)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // Corrected typo: mutableStateOf
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to BookNest", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Login") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    Firebase.auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Register") }
        }
    }
}

enum class AppScreen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Saved("Saved", Icons.AutoMirrored.Filled.List),
    Drafts("Drafts", Icons.Filled.Edit),
    Profile("Profile", Icons.Filled.Person),
    Search("Search", Icons.Filled.Search),
    BookDetail("Details", Icons.Filled.Info),
    Settings("Settings", Icons.Filled.Settings),
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
                    AppScreen.Search -> AppScreen.Home
                    AppScreen.BookDetail -> AppScreen.Home
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
fun DraftsScreen(onNavigate: (AppScreen, String?) -> Unit, books: List<Book>, onEdit: (String) -> Unit, onDelete: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            "My Drafts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        if (books.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(books) { book ->
                    ListItem(
                        headlineContent = { Text(book.title) },
                        leadingContent = { Icon(Icons.Filled.Edit, contentDescription = "Draft") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEdit(book.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { onDelete(book.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier
                            .clickable { onEdit(book.id) }
                            .padding(horizontal = 16.dp)
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No drafts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
            imageVector = Icons.Filled.Info,
            contentDescription = "Cover of ${currentBook.title}",
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text(currentBook.content)
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
            Text("Search Results will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
