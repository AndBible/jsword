package org.crosswire.ksword.k5b

import org.crosswire.common.util.HttpBody
import org.crosswire.common.util.HttpHead
import org.crosswire.common.util.HttpTransport
import org.crosswire.common.util.IniSection
import org.crosswire.common.util.KUri
import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.WebResource
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.install.InstallManager
import org.crosswire.jsword.book.install.InstallerFactory
import org.crosswire.jsword.book.install.sword.HttpsSwordInstaller
import org.crosswire.jsword.index.IndexManager
import org.crosswire.jsword.index.query.QueryBuilder
import org.crosswire.jsword.index.query.QueryDecorator
import org.crosswire.jsword.index.search.Searcher
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.URI

/**
 * K5b inventory (spec section 5.2): records the observable book facade state over the installed test Sword
 * modules and a local catalog fixture, so that the K5b commonization can prove it changed nothing.
 *
 * Opt-in: `-Dk5b.inventory.write=<dir>` writes, `-Dk5b.inventory.check=<dir>` compares and fails listing every
 * differing line. Neither set: the test is skipped. Needs the test modules in `~/.sword`.
 */
class K5bInventoryTest {
    @Test fun inventory() {
        val write = System.getProperty("k5b.inventory.write")
        val check = System.getProperty("k5b.inventory.check")
        assumeTrue("Set k5b.inventory.write or k5b.inventory.check", write != null || check != null)
        require((write == null) != (check == null)) { "Choose exactly one inventory mode" }
        val books = Books.installed().getBooks()
        check(books.isNotEmpty()) { "No installed modules; install the test modules first" }
        val sections = linkedMapOf(
            "books.tsv" to booksSection(books),
            "keys.tsv" to keysSection(books),
            "drivers.tsv" to driversSection(),
            "catalog.tsv" to catalogSection(),
        )
        val dir = File(write ?: check!!)
        if (write != null) {
            dir.mkdirs()
            sections.forEach { (name, text) -> File(dir, name).writeText(text, Charsets.UTF_8) }
        } else {
            val diffs = differences(dir, sections)
            assertTrue("${diffs.size} differing inventory line(s):\n" + diffs.joinToString("\n"), diffs.isEmpty())
        }
    }

    private fun row(vararg cells: Any?) = cells.joinToString("\t") { clean(it.toString()) } + "\n"

    /** Keeps one record on one line; tabs and line breaks inside a value are escaped. */
    private fun clean(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun booksSection(books: List<Book>): String {
        val sb = StringBuilder()
        books.forEachIndexed { i, book ->
            val md = book.bookMetaData
            val props = propertyKeys(md).sorted().joinToString("\u001f") { key ->
                "$key=" + (md.getValues(key)?.joinToString("\u001e") ?: md.getProperty(key))
            }
            sb.append(row(i, book.initials, book.driver?.getDriverName(), book.bookCategory, md.keyType,
                book.language?.code, book.indexStatus, props))
        }
        return sb.toString()
    }

    /**
     * The full property key set. `SwordBookMetaData.propertyKeys` is deliberately null, so the keys of its
     * private `configAll` conf section are read reflectively (test-only).
     */
    private fun propertyKeys(md: BookMetaData): Set<String> {
        md.propertyKeys?.let { return it }
        val field = generateSequence<Class<*>>(md.javaClass) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }.firstOrNull { it.name == "configAll" }
            ?: error("No property keys for ${md.javaClass.name}")
        field.isAccessible = true
        return (field.get(md) as IniSection).keys.toSet()
    }

    private fun keysSection(books: List<Book>): String {
        val sb = StringBuilder()
        for (book in books) {
            try {
                val list = book.getGlobalKeyList()
                val cardinality = list.getCardinality()
                var first = "-"; var last = "-"; var n = 0; var capped = false
                for (key in list) {
                    if (n >= CAP) { capped = true; break }
                    val s = key.getOsisRef() + "/" + key.getName()
                    if (n == 0) first = s
                    last = s
                    n++
                }
                sb.append(row(book.initials, cardinality, first, last, if (capped) "capped" else "iterated=$n"))
            } catch (t: Throwable) {
                sb.append(row(book.initials, -1, t.javaClass.name, "", ""))
            }
        }
        return sb.toString()
    }

    private fun driversSection(): String {
        val sb = StringBuilder()
        Books.installed().drivers.forEachIndexed { i, d -> sb.append(row("books.driver", i, d.getDriverName())) }
        try {
            val manager = InstallManager()
            manager.installerFactoryNames.forEachIndexed { i, n -> sb.append(row("install.factory", i, n)) }
            manager.installers.keys.forEachIndexed { i, n -> sb.append(row("install.site", i, n)) }
        } catch (t: Throwable) {
            sb.append(row("install.manager.error", t.javaClass.name))
        }
        fun plugins(label: String, names: List<String>) = names.forEachIndexed { i, n -> sb.append(row("plugin.$label", i, n)) }
        plugins("BookDriver", PluginRegistry.entries(BookDriver::class).map { it.name })
        plugins("InstallerFactory", PluginRegistry.entries(InstallerFactory::class).map { it.name })
        plugins("IndexManager", PluginRegistry.entries(IndexManager::class).map { it.name })
        plugins("Searcher", PluginRegistry.entries(Searcher::class).map { it.name })
        plugins("QueryBuilder", PluginRegistry.entries(QueryBuilder::class).map { it.name })
        plugins("QueryDecorator", PluginRegistry.entries(QueryDecorator::class).map { it.name })
        return sb.toString()
    }

    /** An installer whose catalog cache lives in [dir] instead of the JSword project directory. */
    private class FixtureInstaller(private val dir: File) : HttpsSwordInstaller() {
        override fun getCachedIndexDir(): URI = dir.toURI()
        fun cacheFile(): File = File(getCachedIndexFile()!!)
    }

    private fun catalogSection(): String {
        val tmp = java.nio.file.Files.createTempDirectory("k5b-catalog").toFile()
        val savedTransport = WebResource.transport
        WebResource.transport = object : HttpTransport {
            override suspend fun head(uri: KUri, timeoutMs: Int): HttpHead = throw AssertionError("network")
            override suspend fun get(uri: KUri, timeoutMs: Int): HttpBody = throw AssertionError("network")
        }
        try {
            val installer = FixtureInstaller(tmp)
            installer.setHost("k5b.invalid")
            installer.setPackageDirectory("/pub")
            installer.setCatalogDirectory("/pub/x")
            installer.cacheFile().writeBytes(K5bCatalogFixture.committedBytes())
            val sb = StringBuilder()
            for (book in installer.getBooks()) {
                sb.append(row(book.initials, book.bookCategory, book.bookMetaData.getProperty("Version"), book.bookMetaData.getProperty("DataPath")))
            }
            return sb.toString()
        } finally {
            WebResource.transport = savedTransport
            tmp.deleteRecursively()
        }
    }

    private fun differences(dir: File, actual: Map<String, String>): List<String> {
        val out = ArrayList<String>()
        for ((name, text) in actual) {
            val f = File(dir, name)
            if (!f.isFile) { out += "file=$name missing in expected inventory"; continue }
            val exp = f.readText(Charsets.UTF_8).lines()
            val act = text.lines()
            for (i in 0 until maxOf(exp.size, act.size)) {
                val e = exp.getOrNull(i); val a = act.getOrNull(i)
                if (e != a) out += "file=$name line=${i + 1} expected=[${e ?: "<none>"}] actual=[${a ?: "<none>"}]"
            }
        }
        return out
    }

    private companion object { const val CAP = 200_000 }
}
