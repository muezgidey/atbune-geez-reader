package org.atbune.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class Destination(val label: String) {
    HOME("Home"), READ("Read"), SEARCH("Search"), BOOKMARKS("Saved"), SETTINGS("Settings")
}

private data class Palette(val background: Color, val surface: Color, val ink: Color, val burgundy: Color, val muted: Color)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AtbuneApp() {
    val context = LocalContext.current
    val store = remember { ReaderSettingsStore(context) }
    val settings by store.settings.collectAsState(initial = null)
    var book by remember { mutableStateOf<BookDraft?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { context.assets.open("book_draft.json").bufferedReader(Charsets.UTF_8).use { BookParser.parse(it.readText()) } }
            .onSuccess { book = it }
            .onFailure { loadError = it.message ?: "Could not open the bundled book." }
    }

    val userSettings = settings ?: ReaderSettings()
    val palette = if (userSettings.darkTheme) {
        Palette(Color(0xFF211B1B), Color(0xFF302626), Color(0xFFF0E5D8), Color(0xFFD99A91), Color(0xFFC3B3A5))
    } else {
        Palette(Color(0xFFF3EBDD), Color(0xFFFFFBF4), Color(0xFF34251F), Color(0xFF762F38), Color(0xFF796B5D))
    }
    val destination = remember { mutableStateOf(Destination.HOME) }
    var currentPage by remember { mutableIntStateOf(0) }
    var showPagePicker by remember { mutableStateOf(false) }
    LaunchedEffect(settings?.lastPage, book?.pages?.size) {
        val count = book?.pages?.size ?: 21
        currentPage = (settings?.lastPage ?: 0).coerceIn(0, count - 1)
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            ParchmentBackdrop(dark = userSettings.darkTheme)
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text(destination.value.label, color = palette.burgundy, fontWeight = FontWeight.SemiBold) },
                        actions = {
                            if (destination.value == Destination.READ && book != null) {
                                TextButton(onClick = { showPagePicker = true }) {
                                    Text("Page $currentPage / ${book!!.pages.lastIndex}", color = palette.burgundy)
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = palette.surface.copy(alpha = 0.94f)) {
                        Destination.entries.forEach { tab ->
                            val icon = when (tab) {
                                Destination.HOME -> Icons.Default.Home
                                Destination.READ -> Icons.Default.Book
                                Destination.SEARCH -> Icons.Default.Search
                                Destination.BOOKMARKS -> Icons.Default.Bookmark
                                Destination.SETTINGS -> Icons.Default.Settings
                            }
                            NavigationBarItem(
                                selected = destination.value == tab,
                                onClick = { destination.value = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                when {
                    loadError != null -> Text("Book could not be loaded: $loadError", modifier = Modifier.padding(padding).padding(24.dp), color = palette.ink)
                    book == null -> Text("Loading book…", modifier = Modifier.padding(padding).padding(24.dp), color = palette.ink)
                    else -> {
                        val loadedBook = book!!
                        when (destination.value) {
                            Destination.HOME -> HomeScreen(loadedBook, currentPage, palette, Modifier.padding(padding)) {
                                destination.value = Destination.READ
                            }
                            Destination.READ -> ReaderScreen(
                                loadedBook, currentPage, userSettings, palette,
                                isBookmarked = currentPage in userSettings.bookmarks,
                                modifier = Modifier.padding(padding),
                                onPage = { page ->
                                    currentPage = page.coerceIn(0, loadedBook.pages.lastIndex)
                                    scope.launch { store.setLastPage(currentPage) }
                                },
                                onBookmark = { scope.launch { store.toggleBookmark(currentPage) } },
                                onPickPage = { showPagePicker = true },
                            )
                            Destination.SEARCH -> SearchScreen(loadedBook.pages, palette, Modifier.padding(padding)) { page ->
                                currentPage = page
                                scope.launch { store.setLastPage(page) }
                                destination.value = Destination.READ
                            }
                            Destination.BOOKMARKS -> BookmarkScreen(loadedBook.pages, userSettings.bookmarks, palette, Modifier.padding(padding)) { page ->
                                currentPage = page
                                scope.launch { store.setLastPage(page) }
                                destination.value = Destination.READ
                            }
                            Destination.SETTINGS -> SettingsScreen(userSettings, palette, store, Modifier.padding(padding))
                        }
                    }
                }
            }
        }
    }

    if (showPagePicker && book != null) {
        AlertDialog(
            onDismissRequest = { showPagePicker = false },
            title = { Text("Choose a page") },
            text = {
                LazyColumn(Modifier.height(380.dp)) {
                    items(book!!.pages) { page ->
                        TextButton(onClick = {
                            currentPage = page.page
                            scope.launch { store.setLastPage(page.page) }
                            showPagePicker = false
                            destination.value = Destination.READ
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Page ${page.page}   ${page.text.lineSequence().firstOrNull().orEmpty().take(30)}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPagePicker = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun HomeScreen(book: BookDraft, currentPage: Int, palette: Palette, modifier: Modifier = Modifier, onContinue: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(10.dp))
        Text("☦", color = palette.burgundy, fontSize = 42.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(book.title, color = palette.burgundy, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
        Text("Ge’ez Orthodox church book", color = palette.muted, style = MaterialTheme.typography.titleMedium)
        Card(colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.9f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reading progress", color = palette.burgundy, fontWeight = FontWeight.SemiBold)
                Text("${book.pages.size} pages · last opened page $currentPage", color = palette.ink)
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue reading") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.88f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(book.status, color = palette.burgundy, fontWeight = FontWeight.Bold)
                Text("This transcription is an unverified draft. Compare every page with the original source before relying on or sharing its text.", color = palette.ink)
            }
        }
        Text("Source: ${book.source ?: "bundled transcription"}", color = palette.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReaderScreen(
    book: BookDraft,
    pageIndex: Int,
    settings: ReaderSettings,
    palette: Palette,
    isBookmarked: Boolean,
    modifier: Modifier = Modifier,
    onPage: (Int) -> Unit,
    onBookmark: () -> Unit,
    onPickPage: () -> Unit,
) {
    val page = book.pages.getOrNull(pageIndex) ?: return
    val family = if (settings.font == ReaderFont.SERIF) FontFamily.Serif else FontFamily.Default
    val ink = when (settings.textColor) {
        "burgundy" -> palette.burgundy
        "black" -> if (settings.darkTheme) Color(0xFFF7F1E8) else Color(0xFF17120F)
        "brown" -> if (settings.darkTheme) Color(0xFFE4CBB5) else Color(0xFF4C3027)
        else -> palette.ink
    }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Page ${page.page}", color = palette.burgundy, fontWeight = FontWeight.SemiBold)
            Row {
                IconButton(onClick = onPickPage) { Icon(Icons.Default.Book, "Choose page", tint = palette.burgundy) }
                IconButton(onClick = onBookmark) {
                    Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Toggle bookmark", tint = palette.burgundy)
                }
            }
        }
        Card(Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.9f))) {
            SelectionContainer {
                Text(
                    page.text,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    color = ink,
                    fontSize = settings.textSize.sp,
                    lineHeight = (settings.textSize * settings.lineSpacing).sp,
                    fontFamily = family,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onPage(pageIndex - 1) }, enabled = pageIndex > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous page", tint = palette.burgundy)
            }
            Text("${pageIndex + 1} of ${book.pages.size}", color = palette.muted)
            IconButton(onClick = { onPage(pageIndex + 1) }, enabled = pageIndex < book.pages.lastIndex) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next page", tint = palette.burgundy)
            }
        }
    }
}

@Composable
private fun SearchScreen(pages: List<BookPage>, palette: Palette, modifier: Modifier = Modifier, onOpenPage: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = remember(pages, query) { findMatchingPages(pages, query) }
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search Ge’ez text") }, singleLine = true)
        if (query.isNotBlank()) Text("${matches.size} matching page(s)", color = palette.muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches) { page ->
                Card(onClick = { onOpenPage(page.page) }, colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.9f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Page ${page.page}", color = palette.burgundy, fontWeight = FontWeight.SemiBold)
                        Text(page.text.take(180), color = palette.ink, maxLines = 3)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkScreen(pages: List<BookPage>, bookmarks: Set<Int>, palette: Palette, modifier: Modifier = Modifier, onOpenPage: (Int) -> Unit) {
    if (bookmarks.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("No saved pages yet. Bookmark a page while reading.", color = palette.muted) }
    } else {
        LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pages.filter { it.page in bookmarks }) { page ->
                Card(onClick = { onOpenPage(page.page) }, colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.9f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Page ${page.page}", color = palette.burgundy, fontWeight = FontWeight.SemiBold)
                        Text(page.text.lineSequence().firstOrNull().orEmpty(), color = palette.ink, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(settings: ReaderSettings, palette: Palette, store: ReaderSettingsStore, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Dark theme", color = palette.ink, fontWeight = FontWeight.Medium)
                Text("Switch between parchment and night colors", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(settings.darkTheme, { scope.launch { store.setDarkTheme(it) } })
        }
        Divider(color = palette.muted.copy(alpha = 0.25f))
        Text("Typeface", color = palette.burgundy, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderFont.entries.forEach { font ->
                FilterChip(selected = settings.font == font, onClick = { scope.launch { store.setFont(font) } }, label = { Text(if (font == ReaderFont.SYSTEM) "System Ethiopic" else "Serif") })
            }
        }
        Text("No custom Ethiopic font is bundled. These choices use Android system fallback fonts; glyph coverage depends on the phone.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
        Divider(color = palette.muted.copy(alpha = 0.25f))
        Text("Text size: ${settings.textSize} sp", color = palette.ink)
        Slider(settings.textSize.toFloat(), { scope.launch { store.setTextSize(it.toInt()) } }, valueRange = 16f..32f, steps = 15)
        Text("Line spacing: ${"%.2f".format(settings.lineSpacing)}×", color = palette.ink)
        Slider(settings.lineSpacing, { scope.launch { store.setLineSpacing(it) } }, valueRange = 1f..2f, steps = 9)
        Text("Text color", color = palette.burgundy, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("default" to "Theme", "brown" to "Brown", "burgundy" to "Burgundy", "black" to "High contrast").forEach { (key, label) ->
                FilterChip(selected = settings.textColor == key, onClick = { scope.launch { store.setTextColor(key) } }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Reading position and saved pages are stored on this device only.", color = palette.muted)
    }
}

@Composable
private fun ParchmentBackdrop(dark: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val base = if (dark) Color(0xFF211B1B) else Color(0xFFF1E7D6)
        val edge = if (dark) Color(0xFF302626) else Color(0xFFE9D9BF)
        drawRect(Brush.verticalGradient(listOf(edge, base, edge)))
        val motif = if (dark) Color(0xFFD99A91).copy(alpha = 0.045f) else Color(0xFF762F38).copy(alpha = 0.045f)
        val spacing = 54.dp.toPx()
        var y = 24.dp.toPx()
        while (y < size.height) {
            var x = 18.dp.toPx()
            while (x < size.width) {
                drawCircle(motif, radius = 6.dp.toPx(), center = Offset(x, y), style = Stroke(width = 0.8.dp.toPx()))
                drawLine(motif, Offset(x - 10.dp.toPx(), y), Offset(x + 10.dp.toPx(), y), strokeWidth = 0.7.dp.toPx())
                x += spacing
            }
            y += spacing
        }
    }
}
