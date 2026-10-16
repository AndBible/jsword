/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.install.sword

import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.crosswire.common.progress.Progress
import org.crosswire.common.util.CWClassLoader
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.NetUtilJvm
import org.crosswire.common.util.splitLikeJava
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookFilters
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.install.DownloadCancelledException
import org.crosswire.jsword.book.install.InstallException
import org.crosswire.jsword.book.install.Installer
import org.crosswire.jsword.book.install.InstallerFactory
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import org.junit.Assume
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.net.URI
import java.nio.file.Files
import java.util.Locale
import java.util.zip.GZIPOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Characterization of [AbstractSwordInstaller], [HttpSwordInstaller],
 * [HttpsSwordInstaller] and their factories: pins the behaviour of the Java
 * classes before they are converted to Kotlin. A catalog (mods.d.tar.gz) is built
 * locally from two conf files of ~/.sword and put where the installer expects its
 * cached index, or handed out by a subclass that overrides the protected
 * download method, so no network is needed. Runs against a temporary CWProject
 * home that is restored afterwards. Expected values were derived by reading the Java.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class SwordInstallerCharacterizationTest {
    private var savedInstance: Any? = null
    private var savedHomeProperty: String? = null
    private lateinit var tempHome: File
    private lateinit var work: File

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        val instanceField: Field = CWProject::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        savedInstance = instanceField.get(null)
        savedHomeProperty = System.getProperty("jsword.home")

        tempHome = Files.createTempDirectory("swordinstaller-test").toFile()
        System.setProperty("jsword.home", tempHome.absolutePath)

        // Fresh, not yet established CWProject so that the temporary home is used
        val reset = CWProject::class.java.getDeclaredMethod("reset")
        reset.isAccessible = true
        reset.invoke(null)
        CWProject.instance().setFrontendName("jsword-test")

        work = Files.createTempDirectory("swordinstaller-work").toFile()
    }

    @AfterTest
    @Throws(Exception::class)
    fun tearDown() {
        // A cancelled job may have interrupted this thread
        Thread.interrupted()
        val instanceField: Field = CWProject::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, savedInstance)
        if (savedHomeProperty == null) {
            System.clearProperty("jsword.home")
        } else {
            System.setProperty("jsword.home", savedHomeProperty!!)
        }

        // Re-point the statics that establishing the temporary home changed
        val restored = savedInstance as CWProject
        CWClassLoader.setHome(restored.projectResourceDirs)
        val writable = restored.writableProjectDir
        NetUtilJvm.setURICacheDir(File(File(writable!!), "netcache"))

        deleteRecursively(tempHome)
        deleteRecursively(work)
    }

    /** An installer whose "download" copies the catalog from a local file and records the requests. */
    private class LocalInstaller : HttpSwordInstaller() {
        val requests: MutableList<String> = ArrayList()
        var catalog: ByteArray? = null
        var failure: InstallException? = null
        var remote: URI? = null

        @Throws(InstallException::class)
        override suspend fun download(job: Progress?, dir: String?, file: String?, dest: URI?) {
            requests.add("$dir|$file")
            if (failure != null) {
                throw failure!!
            }
            try {
                Files.write(File(dest!!.path).toPath(), catalog!!)
            } catch (e: IOException) {
                throw InstallException("local copy failed", e)
            }
        }

        override fun toRemoteURI(book: Book): URI? {
            return if (remote != null) remote else super.toRemoteURI(book)
        }
    }

    /** Opens up the protected download method of the https installer. */
    private class ExposedHttpsInstaller : HttpsSwordInstaller() {
        @Throws(InstallException::class)
        suspend fun exposedDownload(job: Progress?, dir: String?, file: String?, dest: URI?) {
            download(job, dir, file, dest)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCachedIndexListsBothBooksSorted() {
        val installer = installer("cache.invalid")
        putCache(installer, catalog("kjv.conf", "strongsgreek.conf"))

        val books = installer.getBooks()
        assertEquals(listOf("KJV", "StrongsGreek"), initials(books))
        assertTrue(loaded(installer))
        // A fresh list each time
        assertNotSame(books, installer.getBooks())
        books.clear()
        assertEquals(2, installer.getBooks().size)

        val kjv = installer.getBook("KJV")!!
        assertEquals(confValue(conf("kjv.conf"), "Version"), kjv.bookMetaData.getProperty("Version"))
        assertEquals("2.3", kjv.bookMetaData.getProperty("Version"))
        assertEquals("StrongsGreek", installer.getBook("StrongsGreek")!!.initials)
    }

    @Test
    @Throws(Exception::class)
    fun testCachedIndexLocation() {
        val installer = installer("cache.invalid")
        val dir = File(cachedIndexDir(installer).path)
        assertEquals("download-cache.invalid_cat", dir.name)
        assertTrue(dir.isDirectory)
        assertTrue(dir.path.startsWith(tempHome.canonicalPath)
                || dir.path.startsWith(tempHome.absolutePath))
        assertEquals(File(dir, "mods.d.tar.gz").path, cachedIndexFile(installer).path)
    }

    @Test
    @Throws(Exception::class)
    fun testGetBookLookup() {
        val installer = installer("lookup.invalid")
        putCache(installer, catalog("kjv.conf", "strongsgreek.conf"))

        assertNull(installer.getBook("NOPE"))
        assertNull(installer.getBook(""))
        val kjv = installer.getBook("KJV")!!
        assertEquals("KJV", kjv.initials)
        // case insensitive initials
        assertEquals("KJV", installer.getBook("kjv")!!.initials)
        // by name, exact and case insensitive
        assertEquals("KJV", installer.getBook(kjv.name)!!.initials)
        assertEquals("KJV", installer.getBook(kjv.name.uppercase(Locale.getDefault()))!!.initials)
        assertEquals("StrongsGreek", installer.getBook("strongsgreek")!!.initials)
    }

    @Test
    @Throws(Exception::class)
    fun testFilteredBooks() {
        val installer = installer("filter.invalid")
        putCache(installer, catalog("kjv.conf", "strongsgreek.conf"))

        val bibles = installer.getBooks(BookFilters.getBibles())
        assertEquals(listOf("KJV"), initials(bibles))
        assertEquals(2, installer.getBooks(BookFilters.getAll()).size)
    }

    @Test
    @Throws(Exception::class)
    fun testBooksOfACatalogHaveNoConfFile() {
        val installer = installer("noconf.invalid")
        putCache(installer, catalog("kjv.conf"))
        val kjv = installer.getBook("KJV")!!
        assertNull((kjv.bookMetaData as SwordBookMetaData).configFile)
        // Without a conf file in the download dir the book is never newer; no remote is asked
        assertFalse(runBlocking { installer.isNewer(kjv) })
    }

    @Test
    @Throws(Exception::class)
    fun testMissingCacheListsNothingUntilTheCatalogIsReloaded() {
        val installer = LocalInstaller()
        installer.setHost("missing.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setCatalogDirectory("/cat")
        installer.catalog = catalog("kjv.conf", "strongsgreek.conf")

        // getBooks never downloads (K5b D4)
        assertTrue(installer.getBooks().isEmpty())
        assertTrue(installer.requests.isEmpty())
        assertFalse(loaded(installer))

        runBlocking { installer.reloadBookList() }
        assertEquals(listOf("KJV", "StrongsGreek"), initials(installer.getBooks()))
        assertEquals(listOf("/cat|mods.d.tar.gz"), installer.requests)
        // Loaded now: no second download
        installer.getBooks()
        assertEquals(1, installer.requests.size)
    }

    @Test
    @Throws(Exception::class)
    fun testReloadBookListReplacesTheList() {
        val installer = LocalInstaller()
        installer.setHost("reload.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setCatalogDirectory("/cat")
        installer.catalog = catalog("kjv.conf", "strongsgreek.conf")
        // getBooks never downloads: the catalog is fetched by an explicit reload
        assertTrue(installer.getBooks().isEmpty())
        runBlocking { installer.reloadBookList() }
        assertEquals(2, installer.getBooks().size)
        val zip = File(File(cachedIndexDir(installer).path), "mods.d.zip")
        assertTrue(zip.isFile)

        installer.catalog = catalog("kjv.conf")
        runBlocking { installer.reloadBookList() }
        assertFalse(loaded(installer))
        assertEquals(2, installer.requests.size)
        assertTrue(zip.isFile)
        assertFalse(File(zip.path + ".old").exists())
        assertEquals(listOf("KJV"), initials(installer.getBooks()))
    }

    @Test
    @Throws(Exception::class)
    fun testFailureOfTheDownloadPropagatesFromReloadAndIsLoggedByGetBooks() {
        val installer = LocalInstaller()
        installer.setHost("fail.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setCatalogDirectory("/cat")
        installer.failure = InstallException("boom")
        try {
            runBlocking { installer.reloadBookList() }
            fail()
        } catch (expected: InstallException) {
            assertSame(installer.failure, expected)
        }
        // getBooks swallows it and answers an empty (modifiable) list
        val books: MutableList<Book?> = installer.getBooks().toCollection(ArrayList<Book?>())
        assertTrue(books.isEmpty())
        books.add(null)
        assertFalse(loaded(installer))
    }

    @Test
    @Throws(Exception::class)
    fun testCancelledDownloadKeepsItsType() {
        val installer = LocalInstaller()
        installer.setHost("cancel.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setCatalogDirectory("/cat")
        val cancelled = DownloadCancelledException(URI.create("http://x/y"))
        installer.failure = cancelled
        try {
            runBlocking { installer.reloadBookList() }
            fail()
        } catch (expected: DownloadCancelledException) {
            assertSame(cancelled, expected)
            assertEquals("Downloading http://x/y was cancelled", expected.message)
        }

        // The same for install: the exception is rethrown as it is
        val other = LocalInstaller()
        other.setHost("cancel2.invalid")
        other.setPackageDirectory("/pkg")
        other.setCatalogDirectory("/cat")
        other.catalog = catalog("kjv.conf")
        runBlocking { other.reloadBookList() }
        val kjv = other.getBook("KJV")!!
        other.failure = cancelled
        try {
            runBlocking { other.install(kjv) }
            fail()
        } catch (expected: DownloadCancelledException) {
            assertSame(cancelled, expected)
        }
        // The download of the package was asked for with the initials and the zip suffix
        assertEquals("/pkg|KJV.zip", other.requests[other.requests.size - 1])
    }

    @Test
    @Throws(Exception::class)
    fun testInstallFailureRethrowsTheInstallException() {
        val installer = LocalInstaller()
        installer.setHost("install.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setCatalogDirectory("/cat")
        installer.catalog = catalog("strongsgreek.conf")
        runBlocking { installer.reloadBookList() }
        val book = installer.getBook("StrongsGreek")!!
        installer.failure = InstallException("no way")
        try {
            runBlocking { installer.install(book, "my-job-id") }
            fail()
        } catch (expected: InstallException) {
            assertSame(installer.failure, expected)
        }
        assertEquals("/pkg|StrongsGreek.zip", installer.requests[installer.requests.size - 1])
    }

    @Test
    @Throws(Exception::class)
    fun testIsNewerComparesTheRemoteWithTheInstalledConf() {
        val installed = Books.installed().getBook("KJV")
        Assume.assumeNotNull(installed)
        val conf = SwordBookMetaDataJvm.getConfigFile(installed!!.bookMetaData as SwordBookMetaData)
        Assume.assumeTrue(conf != null && conf.exists())

        val remote = File(work, "KJV.zip")
        Files.write(remote.toPath(), byteArrayOf(1))
        val installer = LocalInstaller()
        installer.setHost("newer.invalid")
        installer.remote = remote.toURI()

        assertTrue(remote.setLastModified(conf!!.lastModified() + 3600 * 1000L))
        assertTrue(runBlocking { installer.isNewer(installed) })
        assertTrue(remote.setLastModified(conf.lastModified() - 3600 * 1000L))
        assertFalse(runBlocking { installer.isNewer(installed) })
        assertTrue(remote.setLastModified(conf.lastModified()))
        assertFalse(runBlocking { installer.isNewer(installed) })
    }

    @Test
    fun testInstallerDefinition() {
        val installer = HttpSwordInstaller()
        installer.setHost("h.invalid")
        installer.setPackageDirectory("/p")
        installer.setCatalogDirectory("/c")
        installer.setIndexDirectory("/i")
        assertEquals("h.invalid,/p,/c,/i,,", installer.getInstallerDefinition())

        installer.setProxyHost("px")
        installer.setProxyPort(Integer.valueOf(8080))
        assertEquals("h.invalid,/p,/c,/i,px,8080", installer.getInstallerDefinition())
        assertEquals("h.invalid", installer.getHost())
        assertEquals("/p", installer.getPackageDirectory())
        assertEquals("/c", installer.getCatalogDirectory())
        assertEquals("/i", installer.getIndexDirectory())
        assertEquals("px", installer.getProxyHost())
        assertEquals(Integer.valueOf(8080), installer.getProxyPort())

        // An empty proxy host is stored as null
        installer.setProxyHost("")
        assertNull(installer.getProxyHost())
        installer.setProxyPort(null)
        assertNull(installer.getProxyPort())
        assertEquals("h.invalid,/p,/c,/i,,", installer.getInstallerDefinition())
    }

    @Test
    @Throws(Exception::class)
    fun testDefaults() {
        val installer = HttpSwordInstaller()
        assertNull(installer.getHost())
        assertEquals("", installer.getPackageDirectory())
        assertEquals("", installer.getCatalogDirectory())
        assertEquals("", installer.getIndexDirectory())
        assertNull(installer.getProxyHost())
        assertNull(installer.getProxyPort())
        assertFalse(loaded(installer))
        assertEquals("sword-http", installer.getType())
        assertEquals(java.lang.Long.valueOf(0L), runBlocking { installer.indexLastUpdated() })
    }

    @Test
    @Throws(Exception::class)
    fun testSettersInvalidateTheLoadedFlagOnlyOnChange() {
        val installer = LocalInstaller()
        installer.setHost("flag.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setCatalogDirectory("/cat")
        installer.catalog = catalog("kjv.conf")
        installer.setProxyHost("px")
        installer.setProxyPort(Integer.valueOf(3128))
        runBlocking { installer.reloadBookList() }
        installer.getBooks()
        assertTrue(loaded(installer))

        installer.setHost("flag.invalid")
        installer.setPackageDirectory("/pkg")
        installer.setProxyHost("px")
        installer.setProxyPort(Integer.valueOf(3128))
        // the catalog and index directories never reset the flag
        installer.setCatalogDirectory("/other")
        installer.setIndexDirectory("/index")
        assertTrue(loaded(installer))

        installer.setProxyPort(Integer.valueOf(1))
        assertFalse(loaded(installer))
        setLoaded(installer, true)
        installer.setProxyHost("")
        assertFalse(loaded(installer))
        setLoaded(installer, true)
        installer.setPackageDirectory("/pkg2")
        assertFalse(loaded(installer))
        setLoaded(installer, true)
        installer.setHost("flag2.invalid")
        assertFalse(loaded(installer))
    }

    @Test
    @Throws(Exception::class)
    fun testCloseDropsTheLoadedList() {
        val installer = installer("close.invalid")
        putCache(installer, catalog("kjv.conf"))
        assertEquals(1, installer.getBooks().size)
        assertEquals(1, entries(installer).size)
        installer.close()
        assertTrue(entries(installer).isEmpty())
        assertFalse(loaded(installer))
        // and it is simply reloaded from the cache
        assertEquals(1, installer.getBooks().size)
    }

    @Test
    fun testEqualsHashCodeAndCompareTo() {
        val a = installer("a.invalid")
        val a2 = installer("a.invalid")
        val b = installer("b.invalid")
        val aOtherPackage = installer("a.invalid")
        aOtherPackage.setPackageDirectory("/zzz")

        assertEquals(a, a)
        assertEquals(a, a2)
        assertEquals(a.hashCode(), a2.hashCode())
        assertEquals("a.invalid".hashCode() + "/pkg".hashCode(), a.hashCode())
        assertNotEquals(a, b)
        assertNotEquals(a, aOtherPackage)
        assertNotEquals<Any?>(a, null)
        assertNotEquals<Any?>(a, "a.invalid")
        // the catalog directory does not take part
        a2.setCatalogDirectory("/else")
        assertEquals(a, a2)

        // The same host and package are equal for the Https subclass too, but the two classes never are
        val https = HttpsSwordInstaller()
        https.setHost("a.invalid")
        https.setPackageDirectory("/pkg")
        assertNotEquals<Any?>(a, https)
        assertNotEquals<Any?>(https, a)
        assertEquals(a.hashCode(), https.hashCode())

        assertEquals(0, a.compareTo(a2))
        // compareTo only looks at the package directory when the hosts differ (and answers 0 for the same host)
        assertEquals(0, a.compareTo(b))
        assertEquals(0, a.compareTo(aOtherPackage))
        b.setPackageDirectory("/aaa")
        assertTrue(a.compareTo(b) > 0)
        assertTrue(b.compareTo(a) < 0)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoteUriAndType() {
        val kjv = Books.installed().getBook("KJV")
        Assume.assumeNotNull(kjv)
        val http = installer("h.invalid")
        assertEquals("sword-http", http.getType())
        assertEquals(URI.create("http://h.invalid/pkg/KJV.zip"), http.toRemoteURI(kjv!!))

        val https = HttpsSwordInstaller()
        https.setHost("h.invalid")
        https.setPackageDirectory("/pkg")
        assertEquals("sword-https", https.getType())
        assertEquals(URI.create("https://h.invalid/pkg/KJV.zip"), https.toRemoteURI(kjv))

        // an unusable host gives no uri
        val bad = installer("bad host")
        assertNull(bad.toRemoteURI(kjv))
    }

    @Test
    @Throws(Exception::class)
    fun testHttpsDownloadOfAnUnreachableServerIsAnInstallException() {
        val https = ExposedHttpsInstaller()
        https.setHost("localhost")
        https.setPackageDirectory("/pkg")
        // nobody is expected to listen on the default https port of the loopback interface
        assertEquals(java.lang.Long.valueOf(0L), runBlocking { https.indexLastUpdated() })
        val dest = File(work, "x.zip")
        try {
            runBlocking { https.exposedDownload(null, "/dir", "file.zip", dest.toURI()) }
            fail()
        } catch (expected: InstallException) {
            assertEquals("Unable to find: https://localhost/dir/file.zip", expected.message)
        }
    }

    @Test
    fun testFactories() {
        val http: InstallerFactory = HttpSwordInstallerFactory()
        val https: InstallerFactory = HttpsSwordInstallerFactory()
        assertEquals<Class<*>>(HttpSwordInstaller::class.java, http.createInstaller().javaClass)
        assertEquals<Class<*>>(HttpsSwordInstaller::class.java, https.createInstaller().javaClass)

        // host,package,catalog,proxyHost,proxyPort,(unused)
        val six: Installer = http.createInstaller("h.invalid,/pub,/idx,px,3128,")
        assertEquals<Class<*>>(HttpSwordInstaller::class.java, six.javaClass)
        assertEquals("h.invalid,/pub,/idx,,px,3128", six.getInstallerDefinition())
        val sixHttps: Installer = https.createInstaller("h.invalid,/pub,/idx,,,")
        assertEquals<Class<*>>(HttpsSwordInstaller::class.java, sixHttps.javaClass)
        assertEquals("h.invalid,/pub,/idx,,,", sixHttps.getInstallerDefinition())
        // a proxy host without a port
        assertEquals("h.invalid,/pub,/idx,,px,", http.createInstaller("h.invalid,/pub,/idx,px,,").getInstallerDefinition())

        // the old format: host,directory,proxyHost,proxyPort
        val old: Installer = https.createInstaller("h.invalid,/dir,px,8080")
        assertEquals<Class<*>>(HttpsSwordInstaller::class.java, old.javaClass)
        assertEquals("h.invalid,/dir/packages/rawzip,/dir/raw,,px,8080", old.getInstallerDefinition())
        assertEquals("h.invalid,/dir/packages/rawzip,/dir/raw,,,", http.createInstaller("h.invalid,/dir,,").getInstallerDefinition())

        try {
            http.createInstaller("a,b,c")
            fail()
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("a,b,c"), expected.message)
        }
        try {
            https.createInstaller("h,/p,/c,px,notanumber,")
            fail()
        } catch (expected: NumberFormatException) {
            // expected
        }
    }

    companion object {
        private fun deleteRecursively(file: File) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteRecursively(child)
                }
            }
            file.delete()
        }

        @Throws(IOException::class)
        private fun conf(fileName: String): ByteArray {
            val f = File(System.getProperty("user.home"), ".sword/mods.d/$fileName")
            Assume.assumeTrue("missing test module conf $fileName", f.isFile)
            return Files.readAllBytes(f.toPath())
        }

        /** The value of a key=value line of a conf. */
        private fun confValue(conf: ByteArray, key: String): String? {
            for (line in String(conf, Charsets.UTF_8).splitLikeJava(Regex("\r?\n"))) {
                if (line.startsWith("$key=")) {
                    return line.substring(key.length + 1).trim { it <= ' ' }
                }
            }
            return null
        }

        @Throws(IOException::class)
        private fun addEntry(tar: TarArchiveOutputStream, name: String, data: ByteArray) {
            val entry = TarArchiveEntry(name)
            entry.size = data.size.toLong()
            tar.putArchiveEntry(entry)
            tar.write(data)
            tar.closeArchiveEntry()
        }

        /**
         * A catalog with the given confs, plus a directory entry, an entry that is not a conf and
         * an empty conf, which have to be skipped.
         */
        @Throws(IOException::class)
        private fun catalog(vararg confFiles: String): ByteArray {
            val bos = ByteArrayOutputStream()
            val gz = GZIPOutputStream(bos)
            val tar = TarArchiveOutputStream(gz)
            val dir = TarArchiveEntry("mods.d/")
            tar.putArchiveEntry(dir)
            tar.closeArchiveEntry()
            addEntry(tar, "mods.d/readme.txt", "not a conf".toByteArray(Charsets.UTF_8))
            addEntry(tar, "mods.d/empty.conf", ByteArray(0))
            for (confFile in confFiles) {
                addEntry(tar, "mods.d/$confFile", conf(confFile))
            }
            tar.close()
            return bos.toByteArray()
        }

        private fun installer(host: String): HttpSwordInstaller {
            val installer = HttpSwordInstaller()
            installer.setHost(host)
            installer.setPackageDirectory("/pkg")
            installer.setCatalogDirectory("/cat")
            return installer
        }

        @Throws(Exception::class)
        private fun putCache(installer: AbstractSwordInstaller, catalog: ByteArray) {
            val cache = File(cachedIndexFile(installer).path)
            Files.write(cache.toPath(), catalog)
        }

        private fun initials(books: List<Book?>): List<String> {
            val result = ArrayList<String>()
            for (book in books) {
                result.add(book!!.initials)
            }
            result.sort()
            return result
        }

        // The Java test sat in the installer's package and used its protected members directly;
        // Kotlin protected is subclass-only, so the tests read them reflectively.

        private fun field(name: String): Field {
            val field = AbstractSwordInstaller::class.java.getDeclaredField(name)
            field.isAccessible = true
            return field
        }

        private fun loaded(installer: AbstractSwordInstaller): Boolean = field("loaded").getBoolean(installer)

        private fun setLoaded(installer: AbstractSwordInstaller, value: Boolean) = field("loaded").setBoolean(installer, value)

        @Suppress("UNCHECKED_CAST")
        private fun entries(installer: AbstractSwordInstaller): MutableMap<String, Book> =
            field("entries").get(installer) as MutableMap<String, Book>

        private fun cachedIndexDir(installer: AbstractSwordInstaller): URI {
            val method = AbstractSwordInstaller::class.java.getDeclaredMethod("getCachedIndexDir")
            method.isAccessible = true
            return invokeUnwrapped(method, installer) as URI
        }

        private fun cachedIndexFile(installer: AbstractSwordInstaller): URI {
            val method = AbstractSwordInstaller::class.java.getDeclaredMethod("getCachedIndexFile")
            method.isAccessible = true
            return invokeUnwrapped(method, installer) as URI
        }

        /** Reflective call that rethrows what the method threw, as a direct Java call did. */
        private fun invokeUnwrapped(method: java.lang.reflect.Method, target: Any): Any? {
            try {
                return method.invoke(target)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.cause!!
            }
        }
    }
}
