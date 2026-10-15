package org.crosswire.ksword.k5b

import org.crosswire.common.progress.Progress
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.KUri
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.install.InstallException
import org.crosswire.jsword.book.install.sword.FTPMsg
import org.crosswire.jsword.book.install.sword.HttpSwordInstaller
import org.crosswire.jsword.book.install.sword.HttpsSwordInstaller
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pins the behaviour the installers keep through their common-ready conversion (K5b Task 13): the job ids, the unpacking of a package into the download directory and the
 * JSWORD home, the lock around the book list and the `FTPMsg` resource. Runs against a temporary CWProject home and download
 * directory that are restored afterwards; no network.
 */
class InstallerCommonReadyTest {
    private lateinit var savedInstance: Any
    private var savedHomeProperty: String? = null
    private lateinit var tempHome: File
    private lateinit var savedDownloadDir: File
    private lateinit var downloadDir: File

    @Before fun setUp() {
        val instanceField = CWProject::class.java.getDeclaredField("instance").apply { isAccessible = true }
        savedInstance = instanceField.get(null)
        savedHomeProperty = System.getProperty("jsword.home")
        tempHome = Files.createTempDirectory("installer-common-home").toFile()
        System.setProperty("jsword.home", tempHome.absolutePath)
        CWProject::class.java.getDeclaredMethod("reset").apply { isAccessible = true }.invoke(null)
        CWProject.instance().setFrontendName("jsword-test")
        savedDownloadDir = SwordBookPathJvm.getSwordDownloadDir()
        downloadDir = Files.createTempDirectory("installer-common-dl").toFile()
        SwordBookPathJvm.setDownloadDir(downloadDir)
    }

    @After fun tearDown() {
        Thread.interrupted()
        SwordBookPathJvm.setDownloadDir(savedDownloadDir)
        CWProject::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, savedInstance)
        if (savedHomeProperty == null) System.clearProperty("jsword.home") else System.setProperty("jsword.home", savedHomeProperty)
        Books.installed().getBook("K4RawText")?.let { Books.installed().removeBook(it) }
        tempHome.deleteRecursively()
        downloadDir.deleteRecursively()
    }

    private fun fixtureRoot() = File(checkNotNull(System.getProperty("ksword.fixturesDir")), "space dir/ä")

    /** A package zip of the K4RawText fixture, plus a file that belongs into the JSWORD home. */
    private fun packageZip(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun add(name: String, data: ByteArray) { zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry() }
            val root = fixtureRoot()
            add("mods.d/K4RawText.conf", File(root, "mods.d/K4RawText.conf").readBytes())
            File(root, "modules/K4RawText").listFiles()!!.sortedBy { it.name }.forEach { add("modules/K4RawText/${it.name}", it.readBytes()) }
            add("extra/readme.txt", "kept in the jsword home".toByteArray())
        }
        return out.toByteArray()
    }

    /** An installer whose download writes local bytes and records what was asked for. */
    private open class LocalInstaller(val catalog: ByteArray, val zip: ByteArray) : HttpSwordInstaller() {
        val requests = mutableListOf<String>()
        val jobIds = mutableListOf<String?>()
        var failure: InstallException? = null

        init {
            setHost("local.invalid"); setPackageDirectory("/pkg"); setCatalogDirectory("/cat")
        }

        override fun download(job: Progress?, dir: String?, file: String?, dest: KUri?) {
            requests += "$dir|$file"
            jobIds += job?.jobID
            failure?.let { throw it }
            File(dest!!.getPath()!!).writeBytes(if (file == "mods.d.tar.gz") catalog else zip)
        }
    }

    private fun localInstaller() = LocalInstaller(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()), packageZip())

    @Test fun installUnpacksThePackageAndRegistersTheBook() {
        val installer = localInstaller()
        val book = installer.getBook("K4RawText")!!
        installer.install(book)

        // mods.d and modules go to the download directory, everything else to the JSWORD home
        assertTrue(File(downloadDir, "mods.d/K4RawText.conf").isFile)
        assertTrue(File(downloadDir, "modules/K4RawText/nt").isFile)
        assertFalse(File(tempHome, "mods.d").exists())
        assertFalse(File(tempHome, "modules").exists())
        assertEquals("kept in the jsword home", File(tempHome, "extra/readme.txt").readText())
        // the job is named after the book, the package comes from the package directory
        assertEquals(listOf<String?>("RELOAD_BOOK_LIST", "INSTALL_BOOK-K4RawText"), installer.jobIds)
        assertEquals("/pkg|K4RawText.zip", installer.requests.last())
        // the book is registered and reads from the download directory
        val installed = Books.installed().getBook("K4RawText")
        assertNotNull(installed)
        assertEquals(downloadDir.toURI().path, installed!!.bookMetaData.library!!.getPath())
        assertEquals("In the beginning God created the heaven and the earth.",
            installed.getRawText(installed.getKey("Gen.1.1")).trim())
    }

    @Test fun installWithAJobIdUsesItAndAFailureLeavesNothingBehind() {
        val installer = localInstaller()
        val book = installer.getBook("K4RawText")!!
        installer.failure = InstallException("no way")
        try {
            installer.install(book, "my-job")
            fail()
        } catch (expected: InstallException) {
            assertEquals("no way", expected.message)
        }
        assertEquals("my-job", installer.jobIds.last())
        assertNull(Books.installed().getBook("K4RawText"))
        assertFalse(File(downloadDir, "mods.d").exists())
    }

    @Test fun jobIdsAreFormattedLiterally() {
        val installer = localInstaller()
        val book = installer.getBook("K4RawText")!!
        installer.failure = InstallException("stop")
        for (action in listOf({ installer.install(book) }, { installer.downloadSearchIndex(book, KUri("file:/tmp/x.zip")) })) {
            try { action(); fail() } catch (expected: InstallException) { }
        }
        assertEquals("INSTALL_BOOK-K4RawText", installer.jobIds[1])
        assertEquals("DOWNLOAD_SEARCH_INDEX-K4RawText", installer.jobIds[2])
        assertEquals("/pkg/search/jsword/L1|K4RawText.zip", installer.requests.last())
    }

    @Test fun cancellingTheJobInterruptsTheInstallingThread() {
        for (searchIndex in listOf(false, true)) {
            val interrupted = AtomicInteger()
            val installer = object : LocalInstaller(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()), packageZip()) {
                override fun download(job: Progress?, dir: String?, file: String?, dest: KUri?) {
                    if (file == "mods.d.tar.gz") return super.download(job, dir, file, dest)
                    // cancel from another thread while this (the installing) thread blocks
                    Thread { job!!.cancel() }.start()
                    try {
                        Thread.sleep(10_000)
                    } catch (e: InterruptedException) {
                        interrupted.incrementAndGet()
                        throw InstallException("cancelled")
                    }
                }
            }
            val book = installer.getBook("K4RawText")!!
            try {
                if (searchIndex) installer.downloadSearchIndex(book, KUri("file:/tmp/x.zip")) else installer.install(book)
                fail()
            } catch (expected: InstallException) {
                assertEquals("cancelled", expected.message)
            }
            assertEquals(1, interrupted.get())
            Thread.interrupted()
        }
    }

    @Test fun getBooksWithAFilterIsSerialisedWithTheCatalogLoad() {
        val inLoad = AtomicInteger()
        val maxInLoad = AtomicInteger()
        val installer = object : HttpSwordInstaller() {
            override fun loadCachedIndex() {
                val now = inLoad.incrementAndGet()
                maxInLoad.accumulateAndGet(now) { a, b -> maxOf(a, b) }
                Thread.sleep(15)
                inLoad.decrementAndGet() // loaded stays false: every caller loads again
            }
        }
        val start = CountDownLatch(1)
        val threads = List(6) { i ->
            Thread {
                start.await()
                repeat(3) { if (i % 2 == 0) installer.getBooks(null) else installer.getBook("none") }
            }.also { it.start() }
        }
        start.countDown()
        threads.forEach { it.join() }
        assertEquals(1, maxInLoad.get())
    }

    @Test fun remoteUrisUseTheSchemeOfTheInstaller() {
        val book = K5bFixtureBook.create("K4RawText")
        for ((installer, scheme) in listOf(HttpSwordInstaller() to "http", HttpsSwordInstaller() to "https")) {
            installer.setHost("h.invalid"); installer.setPackageDirectory("/pkg")
            assertEquals("$scheme://h.invalid/pkg/K4RawText.zip", installer.toRemoteURI(book).toString())
        }
    }

    @Test fun ftpMessagesComeFromTheEmbeddedBundle() {
        // lookup takes the resource key; the FTPMsg constants only select the "FTPMsg" bundle
        assertEquals("Too many @ symbols in url: x@y", FTPMsg.URL_AT_COUNT.lookup("SwordInstallerFactory.URLAtCount", "x@y"))
        assertEquals("Wrong number of : symbols in url: x", FTPMsg.URL_COLON_COUNT.lookup("SwordInstallerFactory.URLColonCount", "x"))
        assertEquals("Error loading from cache", FTPMsg.AUTH_REFUSED.lookup("SwordInstaller.CacheError"))
    }
}
