package org.crosswire.ksword.k5

import java.util.LinkedList
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.filter.gbf.GBFFilter
import org.crosswire.jsword.book.filter.gbf.GBFTags
import org.crosswire.jsword.book.filter.gbf.Tag
import org.crosswire.jsword.passage.Key
import org.junit.Assert.*
import org.junit.Test

class GbfStackContractTest {
    @Test fun unmatchedFootnoteRetainsLegacyIndexedAccessError() = assertMalformed("<Rf>")

    @Test fun strongsAfterClosedRootRetainsLegacyIndexedAccessError() = assertMalformed("a<Fb><WG1>")

    private fun assertMalformed(raw: String) {
        val book = requireNotNull(Books.installed().getBook("KJV"))
        val error = assertThrows(IndexOutOfBoundsException::class.java) {
            GBFFilter().toOSIS(book, book.getKey("Gen.1.1"), raw)
        }
        assertEquals("Index: 0, Size: 0", error.message)
    }

    @Test fun publicTagAndAllImplementationsKeepLinkedListParameter() {
        val classes = listOf(Tag::class.java) + GBFTags::class.java.declaredClasses.filter {
            Tag::class.java.isAssignableFrom(it) && !java.lang.reflect.Modifier.isAbstract(it.modifiers)
        }
        assertEquals(24, classes.size)
        for (type in classes) {
            val method = type.methods.single { it.name == "updateOsisStack" }
            assertArrayEquals(type.name, arrayOf(Book::class.java, Key::class.java, LinkedList::class.java), method.parameterTypes)
        }
    }
}
