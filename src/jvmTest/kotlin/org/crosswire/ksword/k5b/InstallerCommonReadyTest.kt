package org.crosswire.ksword.k5b

import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.crosswire.common.progress.Progress
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.KUri
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.install.DownloadCancelledException
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

        override suspend fun download(job: Progress?, dir: String?, file: String?, dest: KUri?) {
            requests += "$dir|$file"
            jobIds += job?.jobID
            failure?.let { throw it }
            File(dest!!.getPath()!!).writeBytes(if (file == "mods.d.tar.gz") catalog else zip)
        }
    }

    /** getBooks never downloads (K5b D4): the catalog is loaded by an explicit reloadBookList(), the job id log starts with it. */
    private suspend fun localInstaller() = LocalInstaller(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()), packageZip())
        .also { it.reloadBookList() }

    @Test fun installUnpacksThePackageAndRegistersTheBook() = runBlocking<Unit> {
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

    @Test fun installWithAJobIdUsesItAndAFailureLeavesNothingBehind() = runBlocking<Unit> {
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

    @Test fun jobIdsAreFormattedLiterally() = runBlocking<Unit> {
        val installer = localInstaller()
        val book = installer.getBook("K4RawText")!!
        installer.failure = InstallException("stop")
        val actions = listOf<suspend () -> Unit>({ installer.install(book) }, { installer.downloadSearchIndex(book, KUri("file:/tmp/x.zip")) })
        for (action in actions) {
            try { action(); fail() } catch (expected: InstallException) { }
        }
        assertEquals("INSTALL_BOOK-K4RawText", installer.jobIds[1])
        assertEquals("DOWNLOAD_SEARCH_INDEX-K4RawText", installer.jobIds[2])
        assertEquals("/pkg/search/jsword/L1|K4RawText.zip", installer.requests.last())
    }

    /** An installer whose package download waits until it is cancelled, as a transfer blocked on the network would. */
    private fun blockedInstaller(started: kotlinx.coroutines.CompletableDeferred<Progress>, cancelled: AtomicInteger) =
        object : LocalInstaller(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()), packageZip()) {
            override suspend fun download(job: Progress?, dir: String?, file: String?, dest: KUri?) {
                if (file == "mods.d.tar.gz") return super.download(job, dir, file, dest)
                started.complete(job!!)
                try {
                    kotlinx.coroutines.awaitCancellation()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    cancelled.incrementAndGet()
                    throw e
                }
            }
        }

    /**
     * (a) A user cancel through the Progress API stops the transfer and surfaces as DownloadCancelledException (the
     * pre-K5b type and message) while the CALLER's coroutine stays active and can keep working.
     */
    @Test fun cancellingTheJobThrowsDownloadCancelledAndLeavesTheCallerActive() = runBlocking<Unit> {
        for (searchIndex in listOf(false, true)) {
            val cancelled = AtomicInteger()
            val started = kotlinx.coroutines.CompletableDeferred<Progress>()
            val installer = blockedInstaller(started, cancelled).also { it.reloadBookList() }
            val book = installer.getBook("K4RawText")!!
            val outcome = kotlinx.coroutines.CompletableDeferred<String>()
            val worker = launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    if (searchIndex) installer.downloadSearchIndex(book, KUri("file:/tmp/x.zip")) else installer.install(book)
                    outcome.complete("no exception")
                } catch (e: DownloadCancelledException) {
                    val activeAfterCancel = isActive
                    kotlinx.coroutines.delay(10) // the caller's next suspension point must not throw
                    outcome.complete("${e.message}|active=$activeAfterCancel|continued")
                } catch (e: Throwable) {
                    outcome.complete("wrong exception $e")
                }
            }
            val job = kotlinx.coroutines.withTimeout(5_000) { started.await() }
            job.cancel() // the Progress API, as the UI's cancel button does
            val result = kotlinx.coroutines.withTimeout(5_000) { outcome.await() }
            assertEquals("Downloading http://local.invalid/pkg/K4RawText.zip was cancelled|active=true|continued", result)
            kotlinx.coroutines.withTimeout(5_000) { worker.join() }
            assertTrue("the caller must not have been cancelled", !worker.isCancelled)
            assertEquals(1, cancelled.get())
            assertTrue(job.isFinished)
            // the module is not installed and no partial download is registered
            assertNull(Books.installed().getBook("K4RawText"))
        }
    }

    /**
     * (c) A Progress cancel after the download phase (here: the moment the job turns non-cancelable) cancels nothing:
     * the install returns normally and the caller's next suspension point does not throw. The bridge is disarmed when
     * the download phase ends.
     */
    @Test fun cancellingTheJobAfterTheDownloadPhaseCancelsNothing() = runBlocking<Unit> {
        var fired = false
        val installer = object : LocalInstaller(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()), packageZip()) {
            override suspend fun download(job: Progress?, dir: String?, file: String?, dest: KUri?) {
                if (file != "mods.d.tar.gz") {
                    // a per-job listener fires when install turns the job non-cancelable, i.e. the download phase is over
                    (job as org.crosswire.common.progress.Job).addWorkListener(object : org.crosswire.common.progress.WorkListener {
                        override fun workProgressed(ev: org.crosswire.common.progress.WorkEvent) {}
                        override fun workStateChanged(ev: org.crosswire.common.progress.WorkEvent) {
                            if (!fired && !ev.job.isCancelable) {
                                fired = true
                                ev.job.cancel()
                            }
                        }
                    })
                }
                super.download(job, dir, file, dest)
            }
        }.also { it.reloadBookList() }
        val book = installer.getBook("K4RawText")!!
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            installer.install(book)
            val stillActive = isActive
            kotlinx.coroutines.delay(10)
            stillActive
        }
        assertTrue(fired)
        assertTrue(result)
    }

    /** (d) An internal failure never cancels the caller. */
    @Test fun anInternalFailureDoesNotCancelTheCaller() = runBlocking<Unit> {
        val installer = localInstaller()
        val book = installer.getBook("K4RawText")!!
        installer.failure = InstallException("no way")
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            try { installer.install(book); "no exception" } catch (e: InstallException) {
                kotlinx.coroutines.delay(10)
                "failed|active=$isActive"
            }
        }
        assertEquals("failed|active=true", result)
    }

    @Test fun cancellingTheCoroutineCancelsTheJob() = runBlocking<Unit> {
        for (searchIndex in listOf(false, true)) {
            val cancelled = AtomicInteger()
            val started = kotlinx.coroutines.CompletableDeferred<Progress>()
            val installer = blockedInstaller(started, cancelled).also { it.reloadBookList() }
            val book = installer.getBook("K4RawText")!!
            val worker = launch(kotlinx.coroutines.Dispatchers.Default) {
                if (searchIndex) installer.downloadSearchIndex(book, KUri("file:/tmp/x.zip")) else installer.install(book)
            }
            val job = kotlinx.coroutines.withTimeout(5_000) { started.await() }
            kotlinx.coroutines.withTimeout(5_000) { worker.cancelAndJoin() }
            assertEquals(1, cancelled.get())
            assertTrue(job.isFinished)
            assertNull(Books.installed().getBook("K4RawText"))
        }
    }

    @Test fun getBooksWithAFilterIsSerialisedWithTheCatalogLoad() = runBlocking<Unit> {
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

    @Test fun remoteUrisUseTheSchemeOfTheInstaller() = runBlocking<Unit> {
        val book = K5bFixtureBook.create("K4RawText")
        for ((installer, scheme) in listOf(HttpSwordInstaller() to "http", HttpsSwordInstaller() to "https")) {
            installer.setHost("h.invalid"); installer.setPackageDirectory("/pkg")
            assertEquals("$scheme://h.invalid/pkg/K4RawText.zip", installer.toRemoteURI(book).toString())
        }
    }

    @Test fun ftpMessagesComeFromTheEmbeddedBundle() = runBlocking<Unit> {
        // lookup takes the resource key; the FTPMsg constants only select the "FTPMsg" bundle
        assertEquals("Too many @ symbols in url: x@y", FTPMsg.URL_AT_COUNT.lookup("SwordInstallerFactory.URLAtCount", "x@y"))
        assertEquals("Wrong number of : symbols in url: x", FTPMsg.URL_COLON_COUNT.lookup("SwordInstallerFactory.URLColonCount", "x"))
        assertEquals("Error loading from cache", FTPMsg.AUTH_REFUSED.lookup("SwordInstaller.CacheError"))
    }
}
