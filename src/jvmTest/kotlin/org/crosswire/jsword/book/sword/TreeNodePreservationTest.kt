package org.crosswire.jsword.book.sword

import java.io.*
import org.junit.Assert.*
import org.junit.Test

class TreeNodePreservationTest {
    @Test fun clonePreservesBackingArrayIdentityAndSerializationFields() {
        val node = TreeNode(12).apply { name = "root"; parent = 3; firstChild = 4; nextSibling = 5; setUserData(byteArrayOf(6, 7)) }
        val clone = node.clone()
        val field = TreeNode::class.java.getDeclaredField("userData").apply { isAccessible = true }
        assertSame(field.get(node), field.get(clone))
        val portable = TreeNode::class.java.getDeclaredMethod("portableTreeClone").apply { isAccessible = true }
            .invoke(node) as TreeNode
        assertSame(field.get(node), field.get(portable))
        assertEquals(node.offset, portable.offset); assertEquals(node.name, portable.name)
        assertEquals(node.parent, portable.parent); assertEquals(node.firstChild, portable.firstChild)
        assertEquals(node.nextSibling, portable.nextSibling)
        val extra = mutableListOf("extra")
        val unknown = object : TreeNode(9) { val reference = extra }
        val unknownClone = unknown.clone()
        assertEquals(unknown.javaClass, unknownClone.javaClass)
        assertSame(extra, unknown.javaClass.getDeclaredField("reference").apply { isAccessible = true }.get(unknownClone))
        val view = node.getUserData(); view[0] = 99
        assertArrayEquals(byteArrayOf(6, 7), clone.getUserData())
        val descriptor = ObjectStreamClass.lookup(TreeNode::class.java)
        assertEquals(-2472601787934480762L, descriptor.serialVersionUID)
        assertEquals(listOf("firstChild", "nextSibling", "offset", "parent", "name", "userData"), descriptor.fields.map { it.name })
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(node) }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() as TreeNode }
        assertEquals(node.offset, restored.offset); assertEquals(node.name, restored.name)
        assertEquals(node.parent, restored.parent); assertEquals(node.firstChild, restored.firstChild)
        assertEquals(node.nextSibling, restored.nextSibling); assertArrayEquals(node.getUserData(), restored.getUserData())
    }
}
