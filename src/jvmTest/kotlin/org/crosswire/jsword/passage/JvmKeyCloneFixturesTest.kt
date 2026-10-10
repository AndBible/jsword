package org.crosswire.jsword.passage

import org.crosswire.jsword.book.sword.RawLDBackend
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class JvmKeyCloneFixturesTest {
    private val kjv = Versifications.instance().getVersification("KJV")
    private val gen1 = VerseRangeFactory.fromString(kjv, "Gen 1")
    private val rom3 = VerseRangeFactory.fromString(kjv, "Rom 3:1-5")

    @Test fun rocketCopiesPopulatedCacheReferencesButInvalidatesOnlyItsOwnCaches() {
        val p = RocketPassage(kjv).apply { add(gen1); optimizeReads() }
        val c = p.clone() as RocketPassage
        // These references are part of the copy contract, although normal writes invalidate them.
        for (name in listOf("distinct", "ranged")) {
            val field = RocketPassage::class.java.getDeclaredField(name).apply { isAccessible = true }
            assertTrue(field.get(p) != null)
            assertSame(field.get(p), field.get(c), name)
        }
        assertEquals("Gen.1", c.getOsisRef())
        c.add(rom3)
        assertEquals("Gen.1", p.getOsisRef())
        p.remove(gen1)
        assertTrue(c.contains(gen1.start))
    }

    @Test fun compressedDictionaryBackendCopiesRuntimeClassAndSharesMetadataWithoutOpeningModule() {
        val metadata = SwordBookMetaData(
            "[CloneZLD]\nDescription=Compressed clone fixture\nModDrv=zLD\n".toByteArray(), "CloneZLD"
        )
        val original = org.crosswire.jsword.book.sword.ZLDBackend(metadata)
        val copy = original.clone() as org.crosswire.jsword.book.sword.ZLDBackend
        assertSame<Class<*>>(original.javaClass, copy.javaClass)
        assertNotSame(original, copy)
        assertEquals(original.getName(), copy.getName())
        assertSame(metadata, copy.bookMetaData)
        original.bookMetaData.setProperty("Description", "original mutation")
        assertEquals("original mutation", copy.bookMetaData.getProperty("Description"))
        copy.bookMetaData.setProperty("Description", "copy mutation")
        assertEquals("copy mutation", original.bookMetaData.getProperty("Description"))
    }

    @Test fun rawDictionaryBackendCopiesRuntimeClassAndMetadataWithoutOpeningModule() {
        // Construction and clone do not call initState; reading/indexing would need module files.
        val metadata = SwordBookMetaData(
            "[CloneTest]\nDescription=Clone fixture\nModDrv=RawLD\n".toByteArray(), "CloneTest"
        )
        val p = RawLDBackend<RawLDBackendState>(metadata, 2)
        val c = p.clone()
        assertSame<Class<*>>(RawLDBackend::class.java, c.javaClass)
        assertNotSame(p, c)
        assertEquals("CloneTest", c.getName())
        assertSame(metadata, c.bookMetaData)
    }
}
