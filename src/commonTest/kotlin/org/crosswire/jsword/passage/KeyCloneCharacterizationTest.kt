package org.crosswire.jsword.passage


import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
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
        assertEquals(p::class, c::class, "${p::class.simpleName} class")
        assertNotSame(p, c)
        assertEquals("Gen.1", c.getOsisRef(), "${p::class.simpleName} content")
        assertEquals(p, c)
        assertSame(p.versification, c.versification)
        c.add(rom3)
        assertFalse(p.contains(rom3.start), "original changed by clone")
        p.remove(gen1)
        assertTrue(c.contains(gen1.start), "clone changed by original")
    }

    private fun checkMissingCloneOverride(key: Key) {
        val failure = assertFailsWith<IllegalStateException> { key.clone() }
        assertTrue(failure.message.orEmpty().contains("must override clone()"))
    }

    @Test fun bitwiseSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : BitwisePassage(kjv) {})

    @Test fun rocketSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : RocketPassage(kjv) {})

    @Test fun distinctSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : DistinctPassage(kjv) {})

    @Test fun rangedSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : RangedPassage(kjv) {})

    @Test fun tallySubclassMustOverrideClone() =
        checkMissingCloneOverride(object : PassageTally(kjv) {})

    @Test fun defaultListSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : DefaultKeyList() {})

    @Test fun treeSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : TreeKey("tree") {})

    @Test fun setListSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : SetKeyList(emptySet()) {})

    @Test fun leafSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : DefaultLeafKeyList("leaf") {})

    @Test fun readOnlyListSubclassMustOverrideClone() =
        checkMissingCloneOverride(object : ReadOnlyKeyList(DefaultKeyList(), false) {})

    @Test fun rocketPassage() = checkPassage(RocketPassage(kjv))
    @Test fun bitwisePassage() = checkPassage(BitwisePassage(kjv))
    @Test fun distinctPassage() = checkPassage(DistinctPassage(kjv))
    @Test fun rangedPassage() = checkPassage(RangedPassage(kjv))
    @Test fun passageTally() = checkPassage(PassageTally(kjv))

    private class StatefulTally : PassageTally {
        private var adjustment = intArrayOf(100)

        constructor(v11n: Versification?) : super(v11n)
        private constructor(copyFrom: StatefulTally) : super(copyFrom) {
            adjustment = copyFrom.adjustment.copyOf()
        }

        override var total: Int
            get() = super.total + adjustment[0]
            set(value) { super.total = value - adjustment[0] }

        fun baseTotal(): Int = super.total
        override fun clone(): StatefulTally = StatefulTally(this)
    }

    private class AdjustedGetterTally : PassageTally {
        private val adjustment = intArrayOf(100)

        constructor(v11n: Versification?) : super(v11n)
        private constructor(copyFrom: AdjustedGetterTally) : super(copyFrom)

        override var total: Int
            get() = super.total + adjustment[0]
            set(value) { super.total = value }

        fun baseTotal(): Int = super.total
        override fun clone(): AdjustedGetterTally = AdjustedGetterTally(this)
    }

    @Test fun tallySubclassCopyDoesNotCallSetterBeforeSubclassInitialization() {
        val p = StatefulTally(kjv).apply { total = 137; add(gen1) }
        assertEquals(37, p.baseTotal())
        val c = p.clone()
        assertEquals(p::class, c::class)
        assertNotSame(p, c)
        assertEquals(37, c.baseTotal())
        assertEquals(137, c.total)
        assertEquals("Gen.1", c.getOsisRef())
        c.total = 237
        assertEquals(137, c.baseTotal())
        assertEquals(37, p.baseTotal())
        c.remove(gen1)
        assertTrue(p.contains(gen1.start))
    }

    @Test fun tallySubclassCopyPreservesBaseStateRatherThanOverriddenGetterValue() {
        val p = AdjustedGetterTally(kjv).apply { total = 37 }
        assertEquals(137, p.total)
        assertEquals(37, p.baseTotal())
        val c = p.clone()
        assertEquals(37, c.baseTotal())
        assertEquals(137, c.total)
        c.total = 52
        assertEquals(52, c.baseTotal())
        assertEquals(37, p.baseTotal())
    }

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

    @Test fun readOnlyPassageSharesDelegateAndCopiesIgnorePolicy() {
        for (ignore in listOf(false, true)) {
            val p = BitwisePassage(kjv).apply { add(gen1) }
            val ro = PassageKeyFactory.getReadOnlyPassage(p, ignore)
            val c = ro.clone() as Passage
            assertEquals(ro::class, c::class)
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
        assertEquals(v::class, c::class)
        assertNotSame(v, c)
        assertEquals(v, c)
        assertEquals("Gen.1.1!a", c.getOsisRef())
        assertSame(kjv, c.versification)
        assertEquals(v.ordinal, c.ordinal)
    }

    @Test fun verseRangeCopiesEndpointsCountAndOriginalName() {
        val r = VerseRangeFactory.fromString(kjv, "Rom 3:1-5")
        val c = r.clone()
        assertEquals(r::class, c::class)
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
        assertEquals(p::class, c::class)
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
        assertEquals(leaf::class, c::class)
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
            assertEquals(ro::class, c::class)
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

    @Test fun passageCloneCopiesListenerRegistrationsIntoAnIndependentList() {
        val p = BitwisePassage(kjv)
        val sources = mutableListOf<Any>()
        val listener = object : PassageListener {
            override fun versesAdded(ev: PassageEvent) { sources.add(ev.getSource()) }
            override fun versesRemoved(ev: PassageEvent) { sources.add(ev.getSource()) }
            override fun versesChanged(ev: PassageEvent) { sources.add(ev.getSource()) }
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

}
