package org.crosswire.ksword.k6

import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.ksword.fixtures.fixtureRoot
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The listing-based sweeps must not delete through a directory entry whose name okio parses as a path: a directory
 * literally named `..\..` is returned by `FileSystem.list` as `<dir>/../..`, which is OUTSIDE the listed directory.
 */
class FtsIndexPathSafetyTest {
    private val root = Files.createTempDirectory("fts-pathsafety").toFile()
    private var saved: Array<okio.Path> = emptyArray()
    /** The swept base; its parent [root] and itself are where a bad delete would land (both are ours to lose). */
    private val sandbox = root.resolve("sandbox")
    private val hostile = "..\\.."
    private val managers = ArrayList<FtsIndexManager>()

    @BeforeTest fun setUp() {
        sandbox.mkdirs()
        saved = SwordBookPath.getAugmentPath()
        SwordBookPath.setAugmentPath(arrayOf(fixtureRoot() / "space dir" / "ä"))
    }

    @AfterTest fun tearDown() {
        managers.forEach { it.closeAllIndexes() }
        SwordBookPath.setAugmentPath(saved)
        root.deleteRecursively()
    }

    private fun book(): Book = assertNotNull(Books.installed().getBook("K4RawText"))

    private fun manager(base: File = sandbox) = FtsIndexManager(base.resolve("search").toOkioPath(), base.resolve("lucene").toOkioPath()) { b, k ->
        BookData(b, k).getOsisFragment(false)
    }.also { managers += it }

    /** Creates `<parent>/<hostile>` through java.io (a legal directory name on Linux) with a file inside, and a victim file in [root]. */
    private fun plant(parent: File): File {
        parent.mkdirs()
        val entry = File(parent, hostile)
        assertTrue(entry.mkdir(), "cannot create a directory named $hostile here")
        File(entry, "inside.txt").writeText("x")
        sandbox.resolve("victim2.txt").writeText("precious")
        return File(root, "victim.txt").also { it.writeText("precious") }
    }

    private fun assertIntact(victim: File) {
        assertTrue(victim.exists() && victim.readText() == "precious", "a file outside the swept directory was deleted")
        assertTrue(sandbox.resolve("victim2.txt").exists(), "a file outside the swept directory was deleted")
        assertTrue(sandbox.resolve("search").exists() || sandbox.resolve("lucene").exists())
    }

    @Test fun legacySweepsSkipAnEntryNamedLikeAParentPath() {
        val b = book()
        val victim = plant(sandbox.resolve("lucene/${b.bookMetaData.driverName}"))
        val m = manager()
        m.deleteOrphanLegacyLuceneIndexes(listOf(b))
        assertIntact(victim)
        m.emptyLegacyLuceneIndexes(listOf(b))
        assertIntact(victim)
        // the same entry as the driver directory
        val victim2 = plant(sandbox.resolve("lucene"))
        m.deleteOrphanLegacyLuceneIndexes(listOf(b))
        assertIntact(victim2)
    }

    @Test fun orphanIndexSweepSkipsAnEntryNamedLikeAParentPath() {
        val b = book()
        val dir = sandbox.resolve("search/${b.bookMetaData.driverName}")
        val victim = plant(dir)
        File(dir, "Gone.sqlite").writeText("x")
        val m = manager()
        assertEquals(1, m.deleteOrphanIndexes(listOf(b)))
        assertIntact(victim)
        val victim2 = plant(sandbox.resolve("search"))
        m.deleteOrphanIndexes(listOf(b))
        assertIntact(victim2)
    }

    @Test fun creatingCleanupDoesNotWalkThroughSuchAnEntry() {
        val b = book()
        val victim = plant(sandbox.resolve("search/${b.bookMetaData.driverName}"))
        val outside = File(sandbox, "keep.sqlite.creating").also { it.writeText("outside the search directory") }
        manager() // the constructor deletes .creating leftovers
        assertIntact(victim)
        assertTrue(outside.exists(), "a .creating file outside search/ was deleted")
    }

    @Test fun legacyEmptyingKeepsTheDirectoryAndSkipsTheEntry() {
        val b = book()
        val legacy = sandbox.resolve("lucene/${b.bookMetaData.driverName}/${b.initials}")
        val victim = plant(legacy)
        File(legacy, "segments.gen").writeText("x")
        val m = manager()
        assertEquals(1, m.emptyLegacyLuceneIndexes(listOf(b)))
        assertIntact(victim)
        assertTrue(legacy.isDirectory)
        assertTrue(File(legacy, hostile).exists(), "the hostile entry is left alone")
        assertTrue(!File(legacy, "segments.gen").exists())
    }
}
