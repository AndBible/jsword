package org.crosswire.jsword.index.fts

import okio.FileSystem
import okio.Path
import org.crosswire.common.progress.JobManager
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.IndexStatusEvent
import org.crosswire.jsword.index.IndexStatusListener
import org.crosswire.jsword.index.search.DefaultSearchModifier
import org.crosswire.jsword.passage.NoSuchVerseException
import org.crosswire.ksword.fixtures.fixtureRoot
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** FtsIndexManager over K4's generated fixture modules (K6 task 10); runs on the JVM and on the iOS simulator. */
class FtsIndexManagerTest {
    private val fs = FileSystem.SYSTEM
    private var savedAugmentPath: Array<Path> = emptyArray()
    private val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fts-manager-${Random.nextLong().toString(16)}"
    private val searchDir = root / "search"
    private val luceneDir = root / "lucene"

    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        fs.createDirectories(root)
    }

    @AfterTest fun restore() {
        SwordBookPath.setAugmentPath(savedAugmentPath)
        fs.deleteRecursively(root, mustExist = false)
    }

    private fun book(initials: String): Book = assertNotNull(Books.installed().getBook(initials), initials)

    private fun manager(fragmentOf: (Book, org.crosswire.jsword.passage.Key) -> fi.sykero.kdom.Element =
        { b, k -> BookData(b, k).getOsisFragment(false) }) = FtsIndexManager(searchDir, luceneDir, fragmentOf)

    private fun finalFile(b: Book) = searchDir / b.bookMetaData.driverName!! / "${b.initials}.sqlite"

    private fun statuses(b: Book): MutableList<IndexStatus?> {
        val seen = ArrayList<IndexStatus?>()
        b.addIndexStatusListener(object : IndexStatusListener {
            override fun statusChanged(ev: IndexStatusEvent?) { seen += ev?.indexStatus }
        })
        return seen
    }

    private fun creatingFiles(): List<Path> =
        if (fs.exists(searchDir)) fs.listRecursively(searchDir).filter { it.name.contains(".creating") }.toList() else emptyList()

    @Test fun buildMakesTheBookIndexedAndSearchable() {
        val b = book("K4RawText")
        val seen = statuses(b)
        val m = manager()
        assertFalse(m.isIndexed(b))
        assertTrue(m.needsReindexing(b))
        m.scheduleIndexCreation(b)
        assertEquals(listOf<IndexStatus?>(IndexStatus.SCHEDULED, IndexStatus.CREATING, IndexStatus.DONE), seen)
        assertTrue(m.isIndexed(b))
        assertFalse(m.needsReindexing(b))
        assertTrue(creatingFiles().isEmpty())

        val found = m.getIndex(b).find("beginning")
        assertEquals(3, found.getCardinality())
        assertEquals(listOf("Gen.1.1", "John.1.1", "John.1.2"), found.map { it.getOsisRef() })
        assertEquals(0, m.getIndex(b).find("zzzz").getCardinality())
        assertEquals(0, m.getIndex(b).find(null).getCardinality())
        m.closeAllIndexes()
    }

    @Test fun rankedSearchAndQuerySyntaxAreUnsupported() {
        val b = book("K4RawText")
        val m = manager()
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        index.searchModifier = DefaultSearchModifier().apply { isRanked = true }
        assertEquals("ranked", assertFailsWith<UnsupportedSearchSyntaxException> { index.find("god") }.syntax)
        index.searchModifier = null
        assertFailsWith<UnsupportedSearchSyntaxException> { index.find("god*") }
        m.closeAllIndexes()
    }

    @Test fun failureOnThirdKeyLeavesUndoneAndNoFiles() {
        val b = book("K4RawText")
        val seen = statuses(b)
        var calls = 0
        val m = manager { book, key ->
            if (++calls == 3) throw IllegalStateException("boom")
            BookData(book, key).getOsisFragment(false)
        }
        m.scheduleIndexCreation(b)
        assertEquals(listOf<IndexStatus?>(IndexStatus.SCHEDULED, IndexStatus.CREATING, IndexStatus.UNDONE), seen)
        assertFalse(fs.exists(finalFile(b)))
        assertTrue(creatingFiles().isEmpty())
        assertFalse(m.isIndexed(b))
    }

    @Test fun creatingLeftoversAreDeletedByANewManager() {
        val b = book("K4RawText")
        val stray = finalFile(b).parent!! / "${b.initials}.sqlite.creating"
        fs.createDirectories(stray.parent!!)
        fs.write(stray) { writeUtf8("partial") }
        assertTrue(fs.exists(stray))
        manager()
        assertFalse(fs.exists(stray))
        assertFalse(fs.exists(finalFile(b)))
    }

    @Test fun cancelMidBuildEndsUndoneWithoutFiles() {
        val b = book("K4RawText")
        val seen = statuses(b)
        var calls = 0
        val m = manager { book, key ->
            if (++calls == 2) assertNotNull(JobManager.findJob("CREATE_INDEX-${book.initials}")).cancel()
            BookData(book, key).getOsisFragment(false)
        }
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.UNDONE, seen.last())
        assertFalse(fs.exists(finalFile(b)))
        assertTrue(creatingFiles().isEmpty())
    }

    @Test fun deleteIndexRemovesTheFile() {
        val b = book("K4RawText")
        val m = manager()
        m.scheduleIndexCreation(b)
        m.getIndex(b)
        assertTrue(fs.exists(finalFile(b)))
        m.deleteIndex(b)
        assertFalse(fs.exists(finalFile(b)))
        assertFalse(m.isIndexed(b))
        assertEquals(IndexStatus.UNDONE, b.indexStatus)
    }

    @Test fun needsReindexingWhenAnalyzerIdOrBookVersionChanged() {
        val b = book("K4RawText")
        val m = manager()
        m.scheduleIndexCreation(b)
        for (change in listOf<(FtsIndexMetadata) -> FtsIndexMetadata>(
            { it.copy(analyzerId = "other") }, { it.copy(codec = "other") }, { it.copy(bookVersion = "9.9") },
        )) {
            m.scheduleIndexCreation(b)
            assertFalse(m.needsReindexing(b))
            val store = assertNotNull(FtsIndexStore.open(finalFile(b)))
            store.use { it.writeMetadata(change(it.metadata())) }
            assertTrue(m.needsReindexing(b))
        }
    }

    @Test fun legacyLuceneDirectoriesAreDetectedAndDeleted() {
        val b = book("K4RawText")
        val other = book("K4RawCom")
        val driver = b.bookMetaData.driverName!!
        val m = manager()
        assertFalse(m.hasLegacyLuceneIndex(b))
        fs.createDirectories(luceneDir / driver / b.initials)
        assertTrue(m.hasLegacyLuceneIndex(b))
        fs.createDirectories(luceneDir / driver / "Gone")
        fs.write(luceneDir / driver / "Gone" / "segments.gen") { writeUtf8("x") }
        fs.createDirectories(luceneDir / "OldDriver" / "Gone2")
        assertEquals(2, m.deleteOrphanLegacyLuceneIndexes(listOf(b, other)))
        assertTrue(m.hasLegacyLuceneIndex(b))
        assertFalse(fs.exists(luceneDir / driver / "Gone"))
        assertFalse(fs.exists(luceneDir / "OldDriver"))
        m.deleteLegacyLuceneIndex(b)
        assertFalse(m.hasLegacyLuceneIndex(b))
    }

    /**
     * Review focus 3: non-verse-keyed books keep the baseline behaviour. A dictionary is indexed, and find converts each
     * hit's osisRef with VerseFactory like Lucene's VerseCollector: whatever that parses is returned (the lenient parser
     * reads the key G0025 as a verse), and a ref it rejects fails the whole search with a wrapped NoSuchVerseException.
     */
    @Test fun dictionaryBookBuildsAndFindUsesTheVerseFactoryLikeTheBaseline() {
        val b = book("K4RawLD")
        val m = manager()
        m.scheduleIndexCreation(b)
        assertTrue(m.isIndexed(b))
        FtsIndexStore.open(finalFile(b))!!.use { assertEquals(3, it.allRowIds().size) }
        m.getIndex(b).find("love")
        m.closeAllIndexes()
    }

    @Test fun findFailsWithWrappedNoSuchVerseExceptionForAnUnparseableKey() {
        val path = root / "bad.sqlite"
        val store = FtsIndexStore.create(path)
        store.beginBuild()
        store.addEntry(1, "not a verse", listOf(org.crosswire.jsword.index.analysis.AnalyzedToken("love", 0)), emptyList())
        store.writeMetadata(FtsIndexMetadata(FTS_SCHEMA_VERSION, "simple", SearchTokenCodec.ID, "K4RawText", null, "x"))
        store.commitBuild()
        val index = FtsIndex(book("K4RawText"), store)
        val e = assertFailsWith<BookException> { index.find("love") }
        assertTrue(e.cause is NoSuchVerseException, "cause ${e.cause}")
        index.close()
        assertFailsWith<BookException> { index.find("love") }
    }

    @Test fun treeKeyedBookIndexesItsNodes() {
        val b = book("K4GenBook")
        val m = manager()
        m.scheduleIndexCreation(b)
        assertTrue(m.isIndexed(b))
        FtsIndexStore.open(finalFile(b))!!.use { assertTrue(it.allRowIds().size >= 3, "${it.allRowIds()}") }
    }

    @Test fun installDownloadedIndexIsUnsupported() {
        assertFailsWith<UnsupportedOperationException> { manager().installDownloadedIndex(book("K4RawText"), null) }
    }
}
