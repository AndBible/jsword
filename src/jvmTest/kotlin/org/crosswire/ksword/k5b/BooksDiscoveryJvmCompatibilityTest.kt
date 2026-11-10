package org.crosswire.ksword.k5b

import org.crosswire.jsword.book.AbstractBookList
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.BooksEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Default Kotlin arguments alone do not retain the pre-discovery Java/JVM entry points. */
class BooksDiscoveryJvmCompatibilityTest {
    @Test fun preDiscoveryJvmDescriptorsRemainCallable() {
        val add = Books::class.java.getDeclaredMethod("addBook", Book::class.java)
        add.invoke(Books.installed(), null)
        val event = BooksEvent::class.java.getConstructor(Any::class.java, Book::class.java, Boolean::class.javaPrimitiveType)
            .newInstance(Books.installed(), null, true)
        assertFalse(event.startupDiscovery)
        assertNotNull(AbstractBookList::class.java.getDeclaredMethod("fireBooksChanged",
            Any::class.java, Book::class.java, Boolean::class.javaPrimitiveType))
    }
}
