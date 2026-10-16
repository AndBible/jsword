package org.crosswire.jsword.index

import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException

/** Characterizes BookIndexer's inline create/delete flow on a recording IndexManager and a proxy Book. */
class BookIndexerTest {
    private val listeners = mutableListOf<IndexStatusListener>()
    private val calls = mutableListOf<String>()

    private val book: Book = Proxy.newProxyInstance(Book::class.java.classLoader, arrayOf(Book::class.java)) { _, m, a ->
        when (m.name) {
            "addIndexStatusListener" -> { listeners.add(a[0] as IndexStatusListener); null }
            "removeIndexStatusListener" -> { listeners.remove(a[0] as IndexStatusListener); null }
            else -> null
        }
    } as Book

    private inner class FakeManager(var indexed: Boolean, val failOn: String? = null) : IndexManager {
        private fun fire(status: IndexStatus) = listeners.toList().forEach { it.statusChanged(IndexStatusEvent(book, status)) }
        override fun isIndexed(book: Book?) = indexed
        override fun getIndex(book: Book?): Index = throw UnsupportedOperationException()
        override fun needsReindexing(book: Book?) = false
        override fun scheduleIndexCreation(book: Book?) {
            calls.add("create")
            if (failOn == "create") throw IllegalStateException("boom")
            indexed = true; fire(IndexStatus.DONE)
        }
        override fun deleteIndex(book: Book?) {
            calls.add("delete")
            if (failOn == "delete") throw BookException("boom")
            indexed = false; fire(IndexStatus.UNDONE)
        }
        override var indexPolicy: IndexPolicy? = null
        override fun closeAllIndexes() = Unit
    }

    private fun indexer(m: IndexManager) = object : BookIndexer(book) { init { indexManager = m } }

    @Test
    fun createRunsInlineAndRemovesItsListener() {
        val ix = indexer(FakeManager(indexed = false))
        ix.createIndex()
        assertEquals(listOf("create"), calls)
        assertTrue(listeners.isEmpty())
        assertTrue(ix.isIndexed)
    }

    @Test
    fun deleteRunsInlineAndLeavesIndexerIdle() {
        val ix = indexer(FakeManager(indexed = true))
        ix.deleteIndex()
        assertEquals(listOf("delete"), calls)
        assertTrue(listeners.isEmpty())
        assertFalse(ix.isIndexed)
    }

    @Test
    fun failureDoesNotLeaveTheIndexerBusy() {
        val m = FakeManager(indexed = true, failOn = "delete")
        val ix = indexer(m)
        assertFailsWith<BookException> { ix.deleteIndex() }
        assertTrue(listeners.isEmpty())
        m.indexed = false
        ix.createIndex()
        assertEquals(listOf("delete", "create"), calls)
    }

    @Test
    fun runtimeFailureInCreateAlsoResetsBusyState() {
        val ix = indexer(FakeManager(indexed = false, failOn = "create"))
        assertFailsWith<IllegalStateException> { ix.createIndex() }
        assertTrue(listeners.isEmpty())
        assertFailsWith<IllegalStateException> { ix.createIndex() }
        assertEquals(listOf("create", "create"), calls)
    }
}
