package org.crosswire.jsword.passage

import org.crosswire.jsword.book.readings.ReadingsKey
import java.util.Date
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ReadingsKeyCloneTest {

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
