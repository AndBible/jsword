package org.crosswire.jsword.passage

import org.crosswire.jsword.book.readings.ReadingsKey
import org.crosswire.jsword.book.sword.RawLDBackend
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
import java.util.Date
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/** Pins the current clone contract before K3 replaces Object.clone with explicit copies. */
class KeyCloneCharacterizationTest {
    private val kjv = Versifications.instance().getVersification("KJV")
    private val gen1 = VerseRangeFactory.fromString(kjv, "Gen 1")
    private val rom3 = VerseRangeFactory.fromString(kjv, "Rom 3:1-5")

    private fun checkPassage(p: Passage) {
        p.add(gen1)
        val c = p.clone() as Passage
        assertSame(p.javaClass, c.javaClass, "${p.javaClass.simpleName} class")
        assertNotSame(p, c)
        assertEquals("Gen.1", c.getOsisRef(), "${p.javaClass.simpleName} content")
        assertEquals(p, c)
        assertSame(p.versification, c.versification)
        c.add(rom3)
        assertFalse(p.contains(rom3.start), "original changed by clone")
        p.remove(gen1)
        assertTrue(c.contains(gen1.start), "clone changed by original")
    }

    @Test fun rocketPassage() = checkPassage(RocketPassage(kjv))
    @Test fun bitwisePassage() = checkPassage(BitwisePassage(kjv))
    @Test fun distinctPassage() = checkPassage(DistinctPassage(kjv))
    @Test fun rangedPassage() = checkPassage(RangedPassage(kjv))
    @Test fun passageTally() = checkPassage(PassageTally(kjv))

    @Test fun tallyCopiesCountsAndOrderingWithoutSharingBoard() {
        val p = PassageTally(kjv).apply { add(gen1); add(gen1); add(rom3) }
        p.ordering = PassageTally.Order.TALLY
        val c = p.clone()
        assertEquals(PassageTally.Order.TALLY, c.ordering)
        assertEquals(p.total, c.total)
        assertEquals(2, c.getTallyOf(gen1.start))
        assertEquals(1, c.getTallyOf(rom3.start))
        assertEquals(p.getNameAndTally(), c.getNameAndTally())
        c.add(gen1)
        assertEquals(2, p.getTallyOf(gen1.start))
        assertEquals(3, c.getTallyOf(gen1.start))
        p.remove(rom3)
        assertEquals(1, c.getTallyOf(rom3.start))
    }

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

    @Test fun readOnlyPassageSharesDelegateAndCopiesIgnorePolicy() {
        for (ignore in listOf(false, true)) {
            val p = BitwisePassage(kjv).apply { add(gen1) }
            val ro = PassageKeyFactory.getReadOnlyPassage(p, ignore)
            val c = ro.clone() as Passage
            assertSame(ro.javaClass, c.javaClass)
            assertNotSame(ro, c)
            assertEquals("Gen.1", c.getOsisRef())
            if (ignore) c.add(rom3) else assertFailsWith<IllegalStateException> { c.add(rom3) }
            assertFalse(p.contains(rom3.start))
            p.add(rom3)
            assertTrue(c.contains(rom3.start))
            assertTrue(ro.contains(rom3.start))
        }
    }

    @Test fun synchronizedPassageClonesDelegateIndependently() {
        checkPassage(PassageKeyFactory.getSynchronizedPassage(BitwisePassage(kjv)))
    }

    @Test fun verseCopiesIdentityAndSubIdentifier() {
        val v = Verse(kjv, BibleBook.GEN, 1, 1, "a")
        val c = v.clone()
        assertSame(v.javaClass, c.javaClass)
        assertNotSame(v, c)
        assertEquals(v, c)
        assertEquals("Gen.1.1!a", c.getOsisRef())
        assertSame(kjv, c.versification)
        assertEquals(v.ordinal, c.ordinal)
    }

    @Test fun verseRangeCopiesEndpointsCountAndOriginalName() {
        val r = VerseRangeFactory.fromString(kjv, "Rom 3:1-5")
        val c = r.clone()
        assertSame(r.javaClass, c.javaClass)
        assertNotSame(r, c)
        assertEquals(r, c)
        assertSame(r.start, c.start)
        assertSame(r.end, c.end)
        assertSame(kjv, c.versification)
        assertEquals(5, c.getCardinality())
        assertEquals("Rom.3.1-Rom.3.5", c.getOsisRef())
        assertEquals(r.getName(), c.getName())
    }

    private fun checkSharedList(p: AbstractKeyList, osis: String = "A") {
        val a = DefaultLeafKeyList("a", "A")
        val b = DefaultLeafKeyList("b", "B")
        p.addAll(a)
        val c = p.clone()
        assertSame(p.javaClass, c.javaClass)
        assertNotSame(p, c)
        assertEquals(p, c)
        assertEquals(osis, c.getOsisRef())
        assertSame(a, c.get(0))
        c.addAll(b)
        assertTrue(p.contains(b), "current clone shares its collection")
        p.removeAll(a)
        assertFalse(c.contains(a), "sharing is bidirectional")
        c.setName("copy")
        assertEquals("list", p.getName())
        assertEquals("copy", c.getName())
    }

    @Test fun defaultKeyListSharesCollectionAndParent() {
        val parent = DefaultLeafKeyList("parent")
        val p = DefaultKeyList(parent, "list")
        checkSharedList(p)
        assertSame(parent, p.clone().getParent())
    }

    @Test fun treeKeySharesChildrenAndParent() {
        val parent = DefaultLeafKeyList("parent")
        val p = TreeKey("list", parent)
        checkSharedList(p, "parent/list")
        assertSame(parent, p.clone().getParent())
    }

    @Test fun setKeyListSharesCollectionAndParent() {
        val parent = DefaultLeafKeyList("parent")
        val p = SetKeyList(emptySet(), parent, "list")
        checkSharedList(p)
        assertSame(parent, p.clone().getParent())
    }

    @Test fun leafCopiesNamesAndClonesParentButParentCollectionIsShared() {
        val parent = DefaultKeyList(null, "parent")
        val leaf = DefaultLeafKeyList("x", "X", parent)
        val c = leaf.clone()
        assertSame(leaf.javaClass, c.javaClass)
        assertNotSame(leaf, c)
        assertEquals(leaf, c)
        assertEquals("x", c.getName())
        assertEquals("X", c.getOsisRef())
        assertNotSame(parent, c.getParent())
        val added = DefaultLeafKeyList("added")
        c.getParent()!!.addAll(added)
        assertTrue(parent.contains(added))
    }

    @Test fun readOnlyKeyListClonesWrapperAndDelegateButSharesDelegateCollection() {
        for (ignore in listOf(false, true)) {
            val p = DefaultKeyList(null, "list").apply { addAll(DefaultLeafKeyList("a")) }
            val ro = ReadOnlyKeyList(p, ignore)
            val c = ro.clone()
            assertSame(ro.javaClass, c.javaClass)
            assertNotSame(ro, c)
            assertEquals("a", c.getOsisRef())
            val added = DefaultLeafKeyList("b")
            if (ignore) c.addAll(added) else assertFailsWith<IllegalStateException> { c.addAll(added) }
            assertFalse(p.contains(added))
            p.addAll(added)
            assertTrue(c.contains(added))
            p.setName("renamed")
            assertEquals("renamed", ro.getName())
            assertEquals("list", c.getName(), "delegate itself was cloned")
        }
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

    @Test fun passageCloneCopiesListenerRegistrationsIntoAnIndependentList() {
        val p = BitwisePassage(kjv)
        val sources = mutableListOf<Any>()
        val listener = object : PassageListener {
            override fun versesAdded(ev: PassageEvent) { sources.add(ev.source) }
            override fun versesRemoved(ev: PassageEvent) { sources.add(ev.source) }
            override fun versesChanged(ev: PassageEvent) { sources.add(ev.source) }
        }
        p.addPassageListener(listener)
        val c = p.clone()
        p.add(gen1)
        c.add(rom3)
        assertEquals(2, sources.size)
        assertSame(p, sources[0])
        assertSame(c, sources[1])
        c.removePassageListener(listener)
        c.remove(rom3)
        assertEquals(2, sources.size, "clone registration was removed")
        p.remove(gen1)
        assertEquals(3, sources.size, "original registration remains")
        assertSame(p, sources[2])
    }

    @Test fun readingsKeyClonesNonNullParentButSharesItsCollection() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val parent = DefaultKeyList(null, "readings")
            val a = DefaultLeafKeyList("a")
            parent.addAll(a)
            val p = ReadingsKey("Jan 1, 2026", "1.January", parent)
            val c = p.clone()
            assertSame(ReadingsKey::class.java, c.javaClass)
            assertNotSame(p, c)
            assertEquals(p, c)
            assertEquals("Jan 1, 2026", c.getName())
            assertEquals("1.January", c.getOsisRef())
            assertEquals(0, p.compareTo(c))
            val copiedParent = c.getParent() as DefaultKeyList
            assertNotSame(parent, copiedParent)
            assertEquals("readings", copiedParent.getName())
            copiedParent.setName("copy")
            assertEquals("readings", parent.getName())
            assertEquals("copy", copiedParent.getName())
            val b = DefaultLeafKeyList("b")
            copiedParent.addAll(b)
            assertTrue(parent.contains(b), "parent collection remains shallow-shared")
            parent.removeAll(a)
            assertFalse(copiedParent.contains(a), "sharing is bidirectional")
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test fun readingsKeyKeepsRuntimeClassDateAndLeafFields() {
        val date = Date(0)
        val p = ReadingsKey(date)
        val c = p.clone()
        assertSame(ReadingsKey::class.java, c.javaClass)
        assertNotSame(p, c)
        assertEquals(p, c)
        assertEquals(p.getOsisRef(), c.getOsisRef())
        assertEquals(0, p.compareTo(c))
        assertEquals(p.hashCode(), c.hashCode())
        date.time = 86400000L
        assertEquals(date.hashCode(), c.hashCode(), "date reference is currently shared")
    }
}
