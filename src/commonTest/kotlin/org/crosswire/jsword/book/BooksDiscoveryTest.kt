package org.crosswire.jsword.book

import org.crosswire.jsword.book.sword.NullBackend
import org.crosswire.jsword.book.sword.SwordBook
import org.crosswire.jsword.book.sword.SwordBookMetaData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Discovery origin belongs to the event, never to a global startup interval or mutable book metadata. */
class BooksDiscoveryTest {
    private fun book(initials: String): Book {
        val metadata = SwordBookMetaData("""
            [$initials]
            Description=$initials
            ModDrv=RawText
            Lang=en
        """.trimIndent().encodeToByteArray(), "$initials.conf")
        return SwordBook(metadata, NullBackend())
    }

    @Test fun nestedInstallDuringDiscoveryKeepsItsOwnOrigin() {
        val books = Books.installed()
        val discovered = book("DiscoveryOrigin")
        val installed = book("InstalledDuringDiscovery")
        val events = mutableListOf<Pair<String, Boolean>>()
        val listener = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) {
                events += ev.book!!.initials to ev.startupDiscovery
                if (ev.book === discovered) books.addBook(installed)
            }
            override fun bookRemoved(ev: BooksEvent) = Unit
        }
        books.addBooksListener(listener)
        try {
            books.addBook(discovered, startupDiscovery = true)
            books.addBook(discovered)
            assertEquals(listOf("DiscoveryOrigin" to true, "InstalledDuringDiscovery" to false,
                "DiscoveryOrigin" to false, "InstalledDuringDiscovery" to false), events)
        } finally {
            books.removeBooksListener(listener)
            for (book in listOf(discovered, installed)) if (books.getBook(book.initials) != null) books.removeBook(book)
        }
    }

    @Test fun failedDiscoveryCallbackDoesNotMarkTheNextInstall() {
        val books = Books.installed()
        val discovered = book("FailedDiscoveryOrigin")
        val origins = mutableListOf<Boolean>()
        var fail = true
        val listener = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) {
                if (ev.book !== discovered) return
                origins += ev.startupDiscovery
                if (fail) throw IllegalStateException("registration callback failed")
            }
            override fun bookRemoved(ev: BooksEvent) = Unit
        }
        books.addBooksListener(listener)
        try {
            assertFailsWith<IllegalStateException> { books.addBook(discovered, startupDiscovery = true) }
            fail = false
            books.addBook(discovered)
            assertEquals(listOf(true, false), origins)
        } finally {
            books.removeBooksListener(listener)
            if (books.getBook(discovered.initials) != null) books.removeBook(discovered)
        }
    }
}
