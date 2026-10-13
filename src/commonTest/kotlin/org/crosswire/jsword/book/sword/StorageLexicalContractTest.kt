package org.crosswire.jsword.book.sword

import kotlin.test.*

/** Fixed results from the reviewed Unix File differentials, not Path.resolve guesses. */
class StorageLexicalContractTest {
    @Test fun spellingKeepsDotsAndLiteralBackslashes() {
        assertEquals("dir\\name/./child", StorageLocation("dir\\name///./child//").spelling)
        assertEquals("/", StorageLocation("////").spelling)
        assertEquals("", StorageLocation("").spelling)
        assertNotEquals(StorageLocation("./modules"), StorageLocation("modules"))
        assertEquals(StorageLocation("a//b/"), StorageLocation("a/b"))
        assertEquals("a/b".hashCode() xor 1234321, StorageLocation("a/b").hashCode())
    }

    @Test fun childrenFollowUnixFileConstruction() {
        assertEquals("relative/absolute", storageChild(StorageLocation("relative"), "/absolute").spelling)
        assertEquals("/parent/absolute", storageChild(StorageLocation("/parent"), "/absolute").spelling)
        assertEquals("/absolute", storageChild(StorageLocation(""), "/absolute").spelling)
        assertEquals("/child", storageChild(StorageLocation(""), "child").spelling)
        assertEquals("/", storageChild(StorageLocation(""), "").spelling)
        assertEquals("", storageChild(null, "").spelling)
        assertEquals("child", storageChild(null, "child").spelling)
        assertEquals("parent", storageChild(StorageLocation("parent"), "").spelling)
        assertFailsWith<NullPointerException> { storageChild(null, null) }
        assertEquals("..", storageName(StorageLocation("a/..")))
        assertEquals(StorageLocation("a"), storageParent(StorageLocation("a/..")))
        assertNull(storageParent(StorageLocation("child")))
        assertEquals(StorageLocation("/"), storageParent(StorageLocation("/child")))
    }

    @Test fun defaultsAreAnImmutableSnapshot() {
        val input = mutableMapOf("a" to "before")
        val snapshot = metadataDefaultsView(input)
        input["a"] = "after"
        input["b"] = "new"
        assertEquals(mapOf("a" to "before"), snapshot)
        // Native read-only maps need not implement MutableMap; either way no mutation may succeed.
        if (snapshot is MutableMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            assertFailsWith<UnsupportedOperationException> { (snapshot as MutableMap<String, String>)["a"] = "changed" }
        }
        assertEquals("before", snapshot["a"])
    }
}
