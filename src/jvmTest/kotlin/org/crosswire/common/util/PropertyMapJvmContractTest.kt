package org.crosswire.common.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.LinkedHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class PropertyMapJvmContractTest {
    @Test fun superclassAndInheritedCloneRemainJavaLinkedHashMap() {
        assertTrue(PropertyMap::class.java.superclass == LinkedHashMap::class.java)
        assertTrue(PropertyMap::class.java.getMethod("clone").declaringClass == java.util.HashMap::class.java)
        val defaults = PropertyMap().apply { put("fallback", "default") }
        val original = PropertyMap(defaults).apply { put("b", "two"); put("a", "one") }
        val copy = original.clone() as PropertyMap
        assertNotSame(original, copy)
        assertEquals(PropertyMap::class.java, copy.javaClass)
        assertEquals(listOf("b", "a"), copy.keys.toList())
        copy["b"] = "changed"
        copy.remove("a")
        assertEquals(mapOf("b" to "two", "a" to "one"), original)
        defaults["fallback"] = "shared"
        assertEquals("shared", copy["fallback"])
        assertEquals("shared", original["fallback"])
        assertFalse(copy.containsKey("fallback"))
    }

    @Test fun serializationRetainsOrderDefaultsAndMutableViews() {
        val defaults = PropertyMap().apply { put("fallback", "default") }
        val original = PropertyMap(defaults).apply { put("b", "two"); put("a", "one") }
        val bytes = ByteArrayOutputStream().also { output ->
            ObjectOutputStream(output).use { it.writeObject(original) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as PropertyMap }
        assertEquals(PropertyMap::class.java, restored.javaClass)
        assertEquals(listOf("b", "a"), restored.keys.toList())
        assertEquals(original, restored)
        assertEquals("default", restored["fallback"])
        assertFalse(restored.containsKey("fallback"))
        defaults["fallback"] = "changed"
        assertEquals("default", restored["fallback"])
        assertTrue(restored.keys.remove("b"))
        restored.entries.single().setValue("updated")
        assertEquals(mapOf("a" to "updated"), restored)
        assertEquals(mapOf("b" to "two", "a" to "one"), original)
    }
}
