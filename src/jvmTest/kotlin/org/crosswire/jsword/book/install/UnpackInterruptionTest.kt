package org.crosswire.jsword.book.install

import okio.Buffer
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.StoredZipWriter
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Task 8 (W4): an interrupted [SwordArchive.unpackZipSpelling] must never leave a module `.conf` next to partial data.
 *
 * The archive is the real K4ZTextBook fixture with `mods.d` FIRST, as real module zips are ordered. The interruption at
 * entry k+1 is a real destination I/O failure: a directory occupies the path of that entry's file, so the write fails
 * after the k earlier entries have landed. (Truncating the zip itself fails before any output, because
 * [SwordArchive] reads the whole archive first, so it would not exercise extraction.)
 *
 * Consistency rule after the interruption and a fresh re-scan: either the conf is absent and the book is not in
 * `Books.installed()`, or conf plus every DataPath file are byte-equal to ONE complete generation.
 */
class UnpackInterruptionTest {
    private lateinit var savedInstance: Any
    private var savedHomeProperty: String? = null
    private lateinit var tempHome: File
    private lateinit var savedDownloadDir: File
    private lateinit var work: File

    @Before fun setUp() {
        val instanceField = CWProject::class.java.getDeclaredField("instance").apply { isAccessible = true }
        savedInstance = instanceField.get(null)
        savedHomeProperty = System.getProperty("jsword.home")
        tempHome = Files.createTempDirectory("unpack-int-home").toFile()
        System.setProperty("jsword.home", tempHome.absolutePath)
        CWProject::class.java.getDeclaredMethod("reset").apply { isAccessible = true }.invoke(null)
        CWProject.instance().setFrontendName("jsword-test")
        savedDownloadDir = SwordBookPathJvm.getSwordDownloadDir()
        work = Files.createTempDirectory("unpack-int-work").toFile()
    }

    @After fun tearDown() {
        SwordBookPathJvm.setDownloadDir(savedDownloadDir)
        CWProject::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, savedInstance)
        if (savedHomeProperty == null) System.clearProperty("jsword.home") else System.setProperty("jsword.home", savedHomeProperty)
        Books.installed().getBook(INITIALS)?.let { Books.installed().removeBook(it) }
        tempHome.deleteRecursively()
        work.deleteRecursively()
    }

    private fun fixtureRoot() = File(checkNotNull(System.getProperty("ksword.fixturesDir")), "space dir/ä")

    /** Generation 1 is the fixture as is; generation 2 differs in the conf and in every data file. */
    private fun generation(number: Int): List<Pair<String, ByteArray>> {
        val root = fixtureRoot()
        val conf = File(root, "mods.d/$INITIALS.conf").readBytes()
        val data = File(root, "modules/$INITIALS").listFiles()!!.sortedBy { it.name }
        return if (number == 1) {
            listOf("mods.d/$INITIALS.conf" to conf) + data.map { "modules/$INITIALS/${it.name}" to it.readBytes() }
        } else {
            listOf("mods.d/$INITIALS.conf" to (conf + "\nAbout=generation two\n".toByteArray())) +
                data.map { "modules/$INITIALS/${it.name}" to (it.readBytes() + byteArrayOf(0, 0, 0, 0)) }
        }
    }

    private fun zip(entries: List<Pair<String, ByteArray>>, name: String): File {
        val buffer = Buffer()
        StoredZipWriter.write(buffer, entries)
        return File(work, name).apply { writeBytes(buffer.readByteArray()) }
    }

    private fun rescan() {
        Books.installed().getBook(INITIALS)?.let { Books.installed().removeBook(it) }
        Books.installed().registerDriver(SwordBookDriver.instance())
    }

    private fun matches(dest: File, entries: List<Pair<String, ByteArray>>) = entries.all { (name, bytes) ->
        File(dest, name).let { it.isFile && it.readBytes().contentEquals(bytes) }
    }

    /** Applies the consistency rule, returning "absent", "gen1" or "gen2". */
    private fun assertConsistent(dest: File, label: String, generations: List<List<Pair<String, ByteArray>>>): String {
        rescan()
        val conf = File(dest, "mods.d/$INITIALS.conf")
        if (!conf.exists()) {
            assertNull(Books.installed().getBook(INITIALS), "$label: no conf, yet the book is installed")
            return "absent"
        }
        val index = generations.indexOfFirst { matches(dest, it) }
        if (index < 0) fail("$label: conf present next to partial or mixed data " +
            generations.first().drop(1).map { (n, b) -> n to (File(dest, n).takeIf { it.isFile }?.length() to b.size) })
        assertNotNull(Books.installed().getBook(INITIALS), "$label: complete module is not discovered")
        return "gen${index + 1}"
    }

    @Test fun interruptedUnpackNeverLeavesConfWithPartialData() {
        val entries = generation(1)
        assertEquals("mods.d/$INITIALS.conf", entries.first().first, "mods.d first, as real module zips are ordered")
        for (k in 1 until entries.size) {
            // The first k entries, then a poison entry below a regular file that is not module data: creating its
            // parent directory fails, so the unpack stops after exactly k entries.
            val interrupted = zip(entries.take(k) + ("modules/$INITIALS/blocker/poison" to byteArrayOf(1)), "gen1-$k.zip")
            val dest = File(work, "dest-$k").apply { mkdirs() }
            SwordBookPathJvm.setDownloadDir(dest)
            File(dest, "modules/$INITIALS").mkdirs()
            File(dest, "modules/$INITIALS/blocker").writeText("not a directory")
            val failure = runCatching { SwordArchive.unpackZipSpelling(interrupted.path, dest.path, true) }
            assertTrue(failure.isFailure, "k=$k: the poison entry must make the unpack fail")
            val state = assertConsistent(dest, "k=$k", listOf(entries))
            println("UNPACK-INTERRUPTION k=$k of ${entries.size} -> $state")
        }
    }

    @Test fun interruptedOverwriteKeepsAReadableModule() {
        val old = generation(1)
        val new = generation(2)
        val oldZip = zip(old, "gen1.zip")
        for (k in 1 until new.size) {
            // Generation 2 up to entry k, then a poison entry below the live regular file of entry k+1: creating its
            // parent directory fails, so the unpack stops after exactly k entries without touching that live file.
            val interrupted = zip(new.take(k) + (new[k].first + "/poison" to byteArrayOf(1)), "gen2-$k.zip")
            val dest = File(work, "over-$k").apply { mkdirs() }
            SwordBookPathJvm.setDownloadDir(dest)
            SwordArchive.unpackZipSpelling(oldZip.path, dest.path, true)
            assertEquals("gen1", assertConsistent(dest, "k=$k setup", listOf(old, new)))
            val failure = runCatching { SwordArchive.unpackZipSpelling(interrupted.path, dest.path, true) }
            assertTrue(failure.isFailure, "k=$k: the overwrite must fail at the poison entry")
            val state = assertConsistent(dest, "k=$k", listOf(old, new))
            println("OVERWRITE-INTERRUPTION k=$k of ${new.size} -> $state")
            // Only generation 1 can survive here (generation 2 never completes), and it must still read.
            assertEquals("gen1", state, "k=$k: the installed module was lost")
            val book = assertNotNull(Books.installed().getBook(INITIALS), "k=$k: installed module vanished")
            assertEquals("In the beginning God created the heaven and the earth.",
                book.getRawText(book.getKey("Gen.1.1")).trim(), "k=$k: Gen 1:1 unreadable")
        }
    }

    private companion object { const val INITIALS = "K4ZTextBook" }
}
