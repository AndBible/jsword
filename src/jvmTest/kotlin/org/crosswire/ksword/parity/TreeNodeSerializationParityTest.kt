package org.crosswire.ksword.parity

import java.io.*
import org.crosswire.jsword.book.sword.TreeNode
import org.junit.Assert.*
import org.junit.Test

class TreeNodeSerializationParityTest {
    private fun bytes(value: Any): ByteArray = ByteArrayOutputStream().also { out ->
        ObjectOutputStream(out).use { it.writeObject(value) }
    }.toByteArray()

    private fun read(bytes: ByteArray, loader: ClassLoader): Any =
        object : ObjectInputStream(ByteArrayInputStream(bytes)) {
            override fun resolveClass(desc: ObjectStreamClass): Class<*> = Class.forName(desc.name, false, loader)
        }.use { it.readObject() }

    @Test fun oldAndNewSerializedNodesReadAcrossIsolatedClassLoaders() {
        val oldClass = Oracle.loader.loadClass("org.crosswire.jsword.book.sword.TreeNode")
        val old = oldClass.getDeclaredConstructor(Int::class.javaPrimitiveType).apply { isAccessible = true }.newInstance(12)
        oldClass.getDeclaredMethod("setName", String::class.java).apply { isAccessible = true }.invoke(old, "root")
        oldClass.getDeclaredMethod("setUserData", ByteArray::class.java).apply { isAccessible = true }.invoke(old, byteArrayOf(6, 7))
        val current = TreeNode(12).apply { name = "root"; setUserData(byteArrayOf(6, 7)) }
        val oldToNew = read(bytes(old), TreeNode::class.java.classLoader) as TreeNode
        assertEquals("root", oldToNew.name); assertEquals(12, oldToNew.offset)
        assertArrayEquals(byteArrayOf(6, 7), oldToNew.getUserData())
        val newToOld = read(bytes(current), Oracle.loader)
        assertEquals("root", oldClass.getDeclaredMethod("getName").apply { isAccessible = true }.invoke(newToOld))
        assertEquals(12, oldClass.getDeclaredMethod("getOffset").apply { isAccessible = true }.invoke(newToOld))
        assertArrayEquals(byteArrayOf(6, 7), oldClass.getDeclaredMethod("getUserData").apply { isAccessible = true }.invoke(newToOld) as ByteArray)
        assertArrayEquals(bytes(old), bytes(current))
    }
}
