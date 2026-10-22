package org.atbune.reader

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ReaderSettingsStoreTest {
    @Test fun settingsAndBookmarksPersistAcrossStoreRecreation() = runBlocking {
        val directory = Files.createTempDirectory("atbune-settings-test").toFile()
        val file = directory.resolve("settings.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = ReaderSettingsStore(PreferenceDataStoreFactory.create(firstScope) { file })
        firstStore.setDarkTheme(true)
        firstStore.setFont(ReaderFont.SERIF)
        firstStore.setTextSize(29)
        firstStore.setLineSpacing(1.7f)
        firstStore.setTextColor("burgundy")
        firstStore.setLastPage(12)
        firstStore.toggleBookmark(12)
        firstScope.cancel()

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val reopened = ReaderSettingsStore(PreferenceDataStoreFactory.create(secondScope) { file })
        val persisted = reopened.settings.first()
        assertTrue(persisted.darkTheme)
        assertEquals(ReaderFont.SERIF, persisted.font)
        assertEquals(29, persisted.textSize)
        assertEquals(1.7f, persisted.lineSpacing, 0.001f)
        assertEquals("burgundy", persisted.textColor)
        assertEquals(12, persisted.lastPage)
        assertEquals(setOf(12), persisted.bookmarks)
        secondScope.cancel()
        directory.deleteRecursively()
    }
}
