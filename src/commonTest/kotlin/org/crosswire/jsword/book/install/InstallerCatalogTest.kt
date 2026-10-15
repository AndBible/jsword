package org.crosswire.jsword.book.install

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.crosswire.common.util.HttpBody
import org.crosswire.common.util.HttpHead
import org.crosswire.common.util.HttpTransport
import org.crosswire.common.util.KUri
import org.crosswire.common.util.WebResource
import org.crosswire.common.util.pathUri
import org.crosswire.jsword.book.install.sword.HttpsSwordInstaller
import org.crosswire.ksword.fixtures.fixtureRoot
import org.crosswire.ksword.fixtures.isJvm

/**
 * Reads the committed SWORD catalog (`k5b/mods.d.tar.gz`, the 15 K4 fixture `.conf` files) through the common
 * installer stack, on the JVM and on the iOS simulator (K5b spec section 5.3). The catalog is seeded into the
 * installer's cache file and the transport throws, so the test proves that the catalog is read from the cache
 * without any network access.
 */
class InstallerCatalogTest {
    /** Initials, category, Version and DataPath of every listed book, as recorded by the K5b inventory (`catalog.tsv`). */
    private val expected = listOf(
        listOf("K4ZText4Verse", "Biblical Texts", "1.0", "./modules/K4ZText4Verse"),
        listOf("K4Devotion", "Daily Devotionals", "1.0", "./modules/K4Devotion/K4Devotion"),
        listOf("K4Latin1", "Biblical Texts", "1.0", "./modules/K4Latin1"),
        listOf("K4ZComChapter", "Commentaries", "1.0", "./modules/K4ZComChapter"),
        listOf("K4ZTextChapterLzss", "Biblical Texts", "1.0", "./modules/K4ZTextChapterLzss"),
        listOf("K4ZTextBook", "Biblical Texts", "1.0", "./modules/K4ZTextBook"),
        listOf("K4ZLD", "Dictionaries", "1.0", "./modules/K4ZLD/K4ZLD"),
        listOf("K4Cipher", "Biblical Texts", "1.0", "./modules/K4Cipher"),
        listOf("K4RawText", "Biblical Texts", "1.0", "./modules/K4RawText"),
        listOf("K4GenBook", "General Books", "1.0", "./modules/K4GenBook/K4GenBook"),
        listOf("K4RawLD", "Dictionaries", "1.0", "./modules/K4RawLD/K4RawLD"),
        listOf("K4RawCom", "Commentaries", "1.0", "./modules/K4RawCom"),
        listOf("K4Combining", "Biblical Texts", "1.0", "./modules/K4Combining"),
        listOf("K4RawLD4", "Dictionaries", "1.0", "./modules/K4RawLD4/K4RawLD4"),
    )

    /** An installer whose catalog cache lives in [dir] instead of the JSword project directory. */
    private class FixtureInstaller(private val dir: Path) : HttpsSwordInstaller() {
        override fun getCachedIndexDir(): KUri = pathUri(dir)
        fun cacheFile(): Path = getCachedIndexFile()!!.getPath()!!.toPath()
    }

    private object NoNetwork : HttpTransport {
        override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead = throw AssertionError("network")
        override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody = throw AssertionError("network")
    }

    /** The committed catalog; the iOS runner provisions it as `k5b/` below the fixture root, the JVM reads the resource directory. */
    private fun catalogBytes(): ByteArray {
        val file = if (isJvm) fixtureRoot().parent!! / "k5b" / "mods.d.tar.gz" else fixtureRoot() / "k5b" / "mods.d.tar.gz"
        return FileSystem.SYSTEM.read(file) { readByteArray() }
    }

    @Test fun catalogFromTheCacheListsTheFixtureBooksWithoutNetwork() {
        val fs = FileSystem.SYSTEM
        val tmp = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "k5b-catalog-${Random.nextLong().toULong()}"
        fs.createDirectories(tmp)
        val savedTransport = WebResource.transport
        WebResource.transport = NoNetwork
        try {
            val installer = FixtureInstaller(tmp)
            installer.setHost("k5b.invalid")
            installer.setPackageDirectory("/pub")
            installer.setCatalogDirectory("/pub/x")
            fs.write(installer.cacheFile()) { write(catalogBytes()) }
            val actual = installer.getBooks().map {
                listOf(it.initials, it.bookCategory.toString(), it.bookMetaData.getProperty("Version"), it.bookMetaData.getProperty("DataPath"))
            }
            // K4RawText4 is in the catalog but has no registered SWORD book type, so it is not listed
            assertEquals(expected.map { it[0] }.toSet(), actual.map { it[0] }.toSet())
            assertEquals(expected.sortedBy { it[0] }, actual.sortedBy { it[0] })
        } finally {
            WebResource.transport = savedTransport
            fs.deleteRecursively(tmp)
        }
    }
}
