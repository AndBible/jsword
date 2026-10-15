package org.crosswire.ksword.k5

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.book.sword.StorageLocation
import org.crosswire.jsword.book.sword.state.OpenFileStateManager
import org.crosswire.jsword.book.sword.state.OpenFileStateManagerCharacterizationTest
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class OracleStateIsolationTest {
    @Test fun oracleScopeRestoresExactGlobalObjectsWhenCaptureThrows() {
        val registry = Books.installed()
        val fields = listOf("books", "initials", "names", "driverSet").map {
            Books::class.java.getDeclaredField(it).apply { isAccessible = true }
        }
        val originals = fields.map { it.get(registry) }
        val augment = SwordBookPath::class.java.getDeclaredField("augmentPath").apply { isAccessible = true }
        val manager = OpenFileStateManager::class.java.getDeclaredField("manager").apply { isAccessible = true }
        val originalAugment = augment.get(null)
        val originalManager = manager.get(null)
        val activatedField = org.crosswire.common.activate.Activator::class.java
            .getDeclaredField("activated").apply { isAccessible = true }
        val activated = activatedField.get(null) as List<*>
        val originalActivated = activated.toList()
        val expected = IllegalStateException("capture failed")
        val actual = assertThrows(IllegalStateException::class.java) {
            JswordOracleTest().withExtraModules {
                val book = requireNotNull(Books.installed().getBook("KJV"))
                book.getRawText(book.getKey("Gen.1.1"))
                throw expected
            }
        }
        assertSame(expected, actual)
        fields.zip(originals).forEach { (field, original) -> assertSame(field.name, original, field.get(registry)) }
        assertSame(originalAugment, augment.get(null))
        assertSame(originalManager, manager.get(null))
        assertEquals(originalActivated.size, activated.size)
        originalActivated.zip(activated).forEach { (original, current) -> assertSame(original, current) }
    }

    @Test fun optionalModuleReadMustNotPoisonLaterInstalledModuleState() {
        val registry = Books.installed()
        val installed = registry.getBook("Josephus")
        assumeNotNull(installed)
        val fields = listOf("books", "initials", "names", "driverSet").map {
            Books::class.java.getDeclaredField(it).apply { isAccessible = true }
        }
        val original = fields.map { it.get(registry) }
        val managerField = OpenFileStateManager::class.java
            .getDeclaredField("manager").apply { isAccessible = true }
        val manager = OpenFileStateManager.instance()
        val isolated = OpenFileStateManager.createForTest(60, 60)
        val downloadField = SwordBookPath::class.java
            .getDeclaredField("overrideDownloadDir").apply { isAccessible = true }
        val augmentField = SwordBookPath::class.java.getDeclaredField("augmentPath").apply { isAccessible = true }
        val augment = augmentField.get(null)
        val download = downloadField.get(null)
        val empty = kotlin.io.path.createTempDirectory("oracle-installed").toFile()
        try {
            fields[0].set(registry, java.util.TreeSet<Book>())
            fields[1].set(registry, HashMap<String, Book>())
            fields[2].set(registry, HashMap<String, Book>())
            fields[3].set(registry, HashSet<org.crosswire.jsword.book.BookDriver>())
            managerField.set(null, isolated)
            downloadField.set(null, StorageLocation(empty.path))
            // Recreate the original order: oracle discovery/read, then a separate installation's Book.
            JswordOracleTest().syntheticJosephusRootIsRecordedButRealReadErrorsPropagate()
            registry.addBook(installed)
            OpenFileStateManagerCharacterizationTest().testGenBookStateIsReused()
            // The opposite order must preserve an already pooled state as well.
            JswordOracleTest().syntheticJosephusRootIsRecordedButRealReadErrorsPropagate()
            OpenFileStateManagerCharacterizationTest().testGenBookStateIsReused()
        } finally {
            isolated.shutDown()
            managerField.set(null, manager)
            downloadField.set(null, download)
            augmentField.set(null, augment)
            fields.zip(original).forEach { (field, value) -> field.set(registry, value) }
            empty.deleteRecursively()
        }
    }
}
