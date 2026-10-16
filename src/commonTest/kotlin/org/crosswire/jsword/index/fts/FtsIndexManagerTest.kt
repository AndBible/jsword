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

    /** Every manager, listener and book status a test touched: undone in [restore] (the books are shared singletons). */
    private val managers = ArrayList<FtsIndexManager>()
    private val listeners = ArrayList<Pair<Book, IndexStatusListener>>()
    private val statusBefore = HashMap<Book, IndexStatus?>()

    @BeforeTest fun install() {
        savedAugmentPath = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
        fs.createDirectories(root)
    }

    @AfterTest fun restore() {
        managers.forEach { it.closeAllIndexes() }
        listeners.forEach { (b, l) -> b.removeIndexStatusListener(l) }
        statusBefore.forEach { (b, st) -> st?.let { b.indexStatus = it } }
        SwordBookPath.setAugmentPath(savedAugmentPath)
        fs.deleteRecursively(root, mustExist = false)
    }

    private fun book(initials: String): Book = assertNotNull(Books.installed().getBook(initials), initials).also { statusBefore.getOrPut(it) { it.indexStatus } }

    private fun manager(fragmentOf: (Book, org.crosswire.jsword.passage.Key) -> fi.sykero.kdom.Element =
        { b, k -> BookData(b, k).getOsisFragment(false) }) = FtsIndexManager(searchDir, luceneDir, fragmentOf).also { managers += it }

    private fun finalFile(b: Book) = searchDir / b.bookMetaData.driverName!! / "${b.initials}.sqlite"

    private fun statuses(b: Book): MutableList<IndexStatus?> {
        val seen = ArrayList<IndexStatus?>()
        val l = object : IndexStatusListener {
            override fun statusChanged(ev: IndexStatusEvent?) { seen += ev?.indexStatus }
        }
        b.addIndexStatusListener(l)
        listeners += b to l
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
        assertFailsWith<UnsupportedSearchSyntaxException> { index.find("\"god light\"^2") }
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

    @Test fun aBookWithoutADriverIsNotIndexedAndNeverThrows() {
        val b = book("K4RawText")
        val driverless = object : Book by b {
            override var bookMetaData: org.crosswire.jsword.book.BookMetaData = object : org.crosswire.jsword.book.BookMetaData by b.bookMetaData {
                override val driverName: String? get() = null
            }
        }
        val m = manager()
        assertFalse(m.isIndexed(driverless))
        assertTrue(m.needsReindexing(driverless))
        assertFalse(m.hasLegacyLuceneIndex(driverless))
        m.deleteLegacyLuceneIndex(driverless)
    }

    @Test fun cancelledRebuildKeepsTheLegacyDirectory() {
        val b = book("K4RawText")
        val legacy = luceneDir / b.bookMetaData.driverName!! / b.initials
        fs.createDirectories(legacy)
        var calls = 0
        val m = manager { book, key ->
            if (++calls == 2) assertNotNull(JobManager.findJob("CREATE_INDEX-${book.initials}")).cancel()
            BookData(book, key).getOsisFragment(false)
        }
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.UNDONE, b.indexStatus)
        assertTrue(m.hasLegacyLuceneIndex(b))
        assertTrue(fs.exists(legacy))
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
            { it.copy(schemaVersion = FTS_SCHEMA_VERSION + 1) }, { it.copy(analyzerId = "other") }, { it.copy(codec = "other") }, { it.copy(bookVersion = "9.9") },
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

    /** A copy of [b] under other initials (the Book interface is delegated). */
    private fun renamed(b: Book, name: String): Book = object : Book by b {
        override val initials: String get() = name
    }

    @Test fun safeSegmentKeepsOrdinaryNamesAndNeutralisesHostileOnes() {
        for (ok in listOf("KJV", "ChiUns", "Thai_KJV", "a-b", "v1.2", "a.b.c", "Ääkköset", "耶稣", "a b", "AB:c", "a b:c", "100%", "%zz"))
            assertEquals(ok, FtsIndexManager.safeSegment(ok), ok)
        for (bad in listOf("../x", "a/b", "..", ".", ".hidden", "C:\\x", "C:x", "", "a\u0000b", "a\nb", "../../..", "%2F", "a\\b")) {
            val seg = FtsIndexManager.safeSegment(bad)
            assertTrue(seg.isNotEmpty() && seg != "." && seg != "..", "'$bad' -> '$seg'")
            assertFalse(seg.any { it == '/' || it == '\\' || it.code < 0x20 }, "'$bad' -> '$seg'")
            assertFalse(seg.startsWith("."), "'$bad' -> '$seg'")
        }
        assertEquals("%2E./x".replace("/", "%2F"), FtsIndexManager.safeSegment("../x"))
        // injective: an escape sequence typed by hand differs from the character it spells
        assertTrue(FtsIndexManager.safeSegment("%2F") != FtsIndexManager.safeSegment("/"))
    }

    @Test fun safeSegmentIsInjectiveOverTrickyNames() {
        val names = listOf("", "%", "%%", "%25", "%2F", "%2f", "/", "a/b", "a%2Fb", "%00", "\u0000", "%00%", "\u0000%", "100%", "100%25",
            "..", "%2E.", "%2E", ".", ".x", "%2Ex", "C:x", "C%3Ax", "C:\\x", "C%3A%5Cx", "%5C", "\\", "KJV", "ChiUns", "%e", "%ee", "%eg")
        val out = names.associateWith { FtsIndexManager.safeSegment(it) }
        val byOutput = out.entries.groupBy({ it.value }, { it.key })
        for ((o, ins) in byOutput) assertEquals(1, ins.size, "collision: $ins -> '$o'")
        assertEquals("100%", out.getValue("100%")) // an ordinary '%' stays
        assertEquals("%", out.getValue(""))
    }

    @Test fun hostileInitialsNeverEscapeTheIndexDirectories() {
        val b = book("K4RawText")
        val m = manager()
        for (name in listOf("../x", "a/b", "..", ".hidden", "C:\\x", "C:x", "c:/x", "", "../../../..")) {
            val hostile = renamed(b, name)
            val file = m.finalPathForTest(hostile)
            assertTrue(FtsIndexManager.isStrictlyUnder(searchDir, file), "$name -> $file")
        }
    }

    @Test fun legacyDeleteWithAHostileNameDeletesNothingOutsideTheLuceneDirectory() {
        val b = book("K4RawText")
        val driver = b.bookMetaData.driverName!!
        val victim = root / "victim"
        fs.createDirectories(victim)
        fs.write(victim / "keep.txt") { writeUtf8("precious") }
        fs.createDirectories(luceneDir / driver)
        val m = manager()
        for (name in listOf("../../victim", "../../../victim", "..", "../../..")) {
            m.deleteLegacyLuceneIndex(renamed(b, name))
            m.deleteOrphanLegacyLuceneIndexes(listOf(b, renamed(b, name)))
            m.emptyLegacyLuceneIndexes(listOf(b, renamed(b, name)))
        }
        assertTrue(fs.exists(victim / "keep.txt"))
        assertTrue(fs.exists(luceneDir))
        assertTrue(fs.exists(searchDir.parent!!)) // the whole temp root is still there
    }

    @Test fun anIndexOfAHostileNameIsBuiltInsideTheSearchDirectory() {
        val b = book("K4RawText")
        val m = manager()
        val hostile = renamed(b, "../escape")
        m.scheduleIndexCreation(hostile)
        assertEquals(IndexStatus.DONE, hostile.indexStatus)
        assertFalse(fs.exists(root / "escape.sqlite"))
        assertTrue(fs.listRecursively(searchDir).any { it.name.endsWith(".sqlite") })
        assertTrue(m.isIndexed(hostile))
        m.deleteIndex(hostile)
        assertFalse(m.isIndexed(hostile))
    }

    @Test fun orphanIndexFilesOfUninstalledBooksAreRemoved() {
        val b = book("K4RawText")
        val other = book("K4RawCom")
        val driver = b.bookMetaData.driverName!!
        val m = manager()
        m.scheduleIndexCreation(b)
        assertTrue(fs.exists(finalFile(b)))
        val dir = searchDir / driver
        for (suffix in listOf(".sqlite", ".sqlite.creating", ".sqlite-wal", ".sqlite-journal")) fs.write(dir / "Gone$suffix") { writeUtf8("x") }
        fs.write(dir / "notes.txt") { writeUtf8("not an index") }
        fs.createDirectories(searchDir / "OldDriver")
        fs.write(searchDir / "OldDriver" / "Old.sqlite") { writeUtf8("x") }
        assertEquals(2, m.deleteOrphanIndexes(listOf(b, other)))
        assertTrue(fs.exists(finalFile(b)), "an installed book's index must survive")
        assertTrue(m.isIndexed(b))
        assertEquals(listOf("K4RawText.sqlite", "notes.txt"), fs.list(dir).map { it.name }.sorted())
        assertFalse(fs.exists(searchDir / "OldDriver"))
        assertEquals(0, m.deleteOrphanIndexes(listOf(b)))
    }

    @Test fun installedBooksLegacyContentsAreFreedButTheDirectoryStaysAsTheEngineChangedSignal() {
        val b = book("K4RawText")
        val driver = b.bookMetaData.driverName!!
        val legacy = luceneDir / driver / b.initials
        fs.createDirectories(legacy / "sub")
        fs.write(legacy / "segments.gen") { writeUtf8("x") }
        fs.write(legacy / "sub" / "_0.cfs") { writeUtf8("y") }
        val m = manager()
        assertEquals(1, m.emptyLegacyLuceneIndexes(listOf(b)))
        assertTrue(fs.exists(legacy) && fs.list(legacy).isEmpty())
        assertTrue(m.hasLegacyLuceneIndex(b))
        assertEquals(0, m.emptyLegacyLuceneIndexes(listOf(b)))
        m.scheduleIndexCreation(b)
        assertFalse(m.hasLegacyLuceneIndex(b)) // the rebuild removes the directory itself
    }

    @Test fun legacyDirectoryIsDeletedWhenTheNewIndexIsDone() {
        val b = book("K4RawText")
        val driver = b.bookMetaData.driverName!!
        val m = manager()
        val legacy = luceneDir / driver / b.initials
        fs.createDirectories(legacy)
        fs.write(legacy / "segments.gen") { writeUtf8("x") }
        assertTrue(m.hasLegacyLuceneIndex(b))
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.DONE, b.indexStatus)
        assertFalse(m.hasLegacyLuceneIndex(b))
        assertFalse(fs.exists(legacy))
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
        FtsIndexStore.open(finalFile(b))!!.use { assertEquals(setOf("G0025", "G0026", "G3056"), it.allRowIds().map { r -> it.osisRef(r) }.toSet()) }
        // "world" occurs only in entry G0025 and "love" only in G0026 (K4 manifest), so each find has exactly one hit.
        assertEquals(1, m.getIndex(b).find("world").getCardinality())
        assertEquals(1, m.getIndex(b).find("love").getCardinality())
        m.closeAllIndexes()
    }

    private fun fragment(body: String): fi.sykero.kdom.Element = fi.sykero.kdom.Element("div").apply {
        addContent("$body ")
        addContent(fi.sykero.kdom.Element("w").setAttribute("lemma", "strong:H430").setText("beta"))
        addContent(fi.sykero.kdom.Element("note").setText("gamma"))
        addContent(fi.sykero.kdom.Element("title").setText("delta"))
    }

    /** K6-4: only canonical text is indexed; notes and headings are not; Strong's only when the book has the feature. */
    @Test fun onlyCanonicalTextIsIndexedAndStrongsNeedTheBookFeature() {
        val b = book("K4RawText")
        val m = manager { _, _ -> fragment("alpha") }
        m.scheduleIndexCreation(b)
        val index = m.getIndex(b)
        assertEquals(6, index.find("alpha").getCardinality())
        assertEquals(6, index.find("beta").getCardinality())
        assertEquals(0, index.find("gamma").getCardinality(), "note text")
        assertEquals(0, index.find("delta").getCardinality(), "heading text")
        m.closeAllIndexes()
        FtsIndexStore.open(finalFile(b))!!.use { assertTrue(it.strongs("H0430").isEmpty(), "K4RawText has no Strong's feature") }
    }

    /** A rebuild replaces the file under a cached handle; getIndex must then serve the new data. */
    @Test fun rebuildEvictsTheCachedIndex() {
        val b = book("K4RawText")
        var word = "alpha"
        val m = manager { _, _ -> fragment(word) }
        m.scheduleIndexCreation(b)
        assertEquals(6, m.getIndex(b).find("alpha").getCardinality())
        word = "omega"
        m.scheduleIndexCreation(b)
        assertEquals(6, m.getIndex(b).find("omega").getCardinality())
        assertEquals(0, m.getIndex(b).find("alpha").getCardinality())
        m.closeAllIndexes()
    }

    @Test fun getIndexOfAnUnindexedBookFailsWithAnAccurateMessage() {
        val e = assertFailsWith<BookException> { manager().getIndex(book("K4RawText")) }
        assertFalse(e.message!!.contains("Lucene"), e.message)
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
}
