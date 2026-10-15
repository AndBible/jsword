package org.crosswire.jsword.book.install

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
 * `getBooks()` never touches the network (K5b spec section 4.4 amendment): a repository whose catalog cache is
 * missing lists no books instead of downloading the catalog; the host calls the suspend `reloadBookList()` itself.
 */
class InstallerNoNetworkGetBooksTest {
    private class FixtureInstaller(private val dir: Path) : HttpsSwordInstaller() {
        override fun getCachedIndexDir(): KUri = pathUri(dir)
        fun cacheFile(): Path = getCachedIndexFile()!!.getPath()!!.toPath()
    }

    private class CountingNetwork : HttpTransport {
        var calls = 0
        override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead { calls++; throw okio.IOException("network down") }
        override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody { calls++; throw okio.IOException("network down") }
    }

    private fun catalogBytes(): ByteArray {
        val file = if (isJvm) fixtureRoot().parent!! / "k5b" / "mods.d.tar.gz" else fixtureRoot() / "k5b" / "mods.d.tar.gz"
        return FileSystem.SYSTEM.read(file) { readByteArray() }
    }

    @Test fun missingCacheGivesAnEmptyListWithoutNetworkAndASeededCacheGivesTheBooks() {
        val fs = FileSystem.SYSTEM
        val tmp = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "k5b-nonet-${Random.nextLong().toULong()}"
        fs.createDirectories(tmp)
        val network = CountingNetwork()
        val savedTransport = WebResource.transport
        WebResource.transport = network
        try {
            val installer = FixtureInstaller(tmp)
            installer.setHost("k5b.invalid")
            installer.setPackageDirectory("/pub")
            installer.setCatalogDirectory("/pub/x")
            assertEquals(emptyList(), installer.getBooks())
            assertEquals(emptyList(), installer.getBooks())
            assertEquals(0, network.calls, "getBooks must not download")

            fs.write(installer.cacheFile()) { write(catalogBytes()) }
            val books = installer.getBooks()
            assertEquals(14, books.size)
            assertTrue(books.any { it.initials == "K4Latin1" })
            assertEquals(0, network.calls)
        } finally {
            WebResource.transport = savedTransport
            fs.deleteRecursively(tmp)
        }
    }
}
