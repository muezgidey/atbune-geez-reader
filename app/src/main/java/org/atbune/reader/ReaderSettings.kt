package org.atbune.reader

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "reader_settings")

enum class ReaderFont { SYSTEM, SERIF }

data class ReaderSettings(
    val darkTheme: Boolean = false,
    val font: ReaderFont = ReaderFont.SYSTEM,
    val textSize: Int = 22,
    val lineSpacing: Float = 1.35f,
    val textColor: String = "default",
    val lastPage: Int = 0,
    val bookmarks: Set<Int> = emptySet(),
)

class ReaderSettingsStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.settingsDataStore)
    private object Keys {
        val dark = booleanPreferencesKey("dark_theme")
        val font = stringPreferencesKey("font")
        val size = intPreferencesKey("text_size")
        val spacing = floatPreferencesKey("line_spacing")
        val color = stringPreferencesKey("text_color")
        val page = intPreferencesKey("last_page")
        val bookmarks = stringSetPreferencesKey("bookmarks")
    }

    val settings: Flow<ReaderSettings> = dataStore.data.map { p ->
        ReaderSettings(
            darkTheme = p[Keys.dark] ?: false,
            font = runCatching { ReaderFont.valueOf(p[Keys.font] ?: "SYSTEM") }.getOrDefault(ReaderFont.SYSTEM),
            textSize = (p[Keys.size] ?: 22).coerceIn(16, 32),
            lineSpacing = (p[Keys.spacing] ?: 1.35f).coerceIn(1.0f, 2.0f),
            textColor = p[Keys.color] ?: "default",
            lastPage = p[Keys.page] ?: 0,
            bookmarks = p[Keys.bookmarks].orEmpty().mapNotNull(String::toIntOrNull).toSet(),
        )
    }

    suspend fun setDarkTheme(value: Boolean) = dataStore.edit { it[Keys.dark] = value }
    suspend fun setFont(value: ReaderFont) = dataStore.edit { it[Keys.font] = value.name }
    suspend fun setTextSize(value: Int) = dataStore.edit { it[Keys.size] = value.coerceIn(16, 32) }
    suspend fun setLineSpacing(value: Float) = dataStore.edit { it[Keys.spacing] = value.coerceIn(1.0f, 2.0f) }
    suspend fun setTextColor(value: String) = dataStore.edit { it[Keys.color] = value }
    suspend fun setLastPage(value: Int) = dataStore.edit { it[Keys.page] = value.coerceAtLeast(0) }
    suspend fun toggleBookmark(page: Int) = dataStore.edit { prefs ->
        val updated = prefs[Keys.bookmarks].orEmpty().toMutableSet()
        val key = page.toString()
        if (!updated.add(key)) updated.remove(key)
        prefs[Keys.bookmarks] = updated
    }
}
