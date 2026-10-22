package org.atbune.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class BookDraftTest {
    private val bytes by lazy { requireNotNull(javaClass.getResourceAsStream("/book_draft.json")).use { it.readBytes() } }
    private val book by lazy { BookParser.parse(bytes.toString(Charsets.UTF_8)) }

    @Test fun pageSequenceIsExactlyZeroThroughTwenty() {
        assertEquals((0..20).toList(), book.pages.map { it.page })
    }

    @Test fun everyPageHasContentAndReviewMarker() {
        assertTrue(book.pages.all { it.text.isNotBlank() })
        assertTrue(book.pages.all { it.review_status == "needs_manual_comparison" })
        assertTrue(book.status.startsWith("DRAFT"))
    }

    @Test fun bundledTextAssetIsByteForByteOriginalDraft() {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        assertEquals("B6C94A5B009B40FB45D7408C00A59A710213ADBA875BB68DBE77EB4C8F7BDEF7", sha256)
        assertTrue(book.pages.first().text.startsWith("መልክዐ"))
        assertFalse(book.pages.last().text.isBlank())
    }

    @Test fun searchMatchesGeEzUnicodeWithoutChangingStoredText() {
        val original = book.pages.first().text
        assertEquals(listOf(0, 7), findMatchingPages(book.pages, "ኣቡነ").map { it.page })
        assertEquals(original, book.pages.first().text)
    }
}
