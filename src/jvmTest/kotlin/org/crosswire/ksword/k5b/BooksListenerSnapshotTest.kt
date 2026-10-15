package org.crosswire.ksword.k5b

import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.BooksEvent
import org.crosswire.jsword.book.BooksListener
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the snapshot semantics of `CopyOnWriteArrayList` for `Books` events: a listener that removes itself while
 * the event fires neither breaks the other listeners nor throws, and the removal takes effect for the next event.
 */
class BooksListenerSnapshotTest {
    @Test fun listenerRemovingItselfDuringFireDoesNotBreakOthers() {
        val books = Books.installed()
        val book = K5bFixtureBook.create()
        val seen = mutableListOf<String>()
        lateinit var self: BooksListener
        self = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) { seen += "self"; books.removeBooksListener(self) }
            override fun bookRemoved(ev: BooksEvent) {}
        }
        val other = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) { seen += "other" }
            override fun bookRemoved(ev: BooksEvent) {}
        }
        books.addBook(book)
        books.addBooksListener(self); books.addBooksListener(other)
        try {
            books.removeBook(book); books.addBook(book) // addBook fires bookAdded: self removes itself
            books.removeBook(book); books.addBook(book)
        } finally {
            books.removeBooksListener(other); books.removeBooksListener(self)
            if (books.getBook(book.initials) != null) books.removeBook(book)
        }
        assertEquals(listOf("self", "other", "other"), seen)
    }

    @Test fun listenerAddedDuringFireSeesOnlyLaterEvents() {
        val books = Books.installed()
        val book = K5bFixtureBook.create()
        val seen = mutableListOf<String>()
        val late = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) { seen += "late" }
            override fun bookRemoved(ev: BooksEvent) {}
        }
        val adder = object : BooksListener {
            override fun bookAdded(ev: BooksEvent) { seen += "adder"; books.addBooksListener(late) }
            override fun bookRemoved(ev: BooksEvent) {}
        }
        books.addBooksListener(adder)
        try {
            books.addBook(book)
            books.removeBook(book); books.removeBooksListener(adder)
            books.addBook(book)
        } finally {
            books.removeBooksListener(adder); books.removeBooksListener(late)
            if (books.getBook(book.initials) != null) books.removeBook(book)
        }
        assertEquals(listOf("adder", "late"), seen)
    }
}
