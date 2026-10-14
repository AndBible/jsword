package org.crosswire.common.util

actual open class PropertyMapSuperclass<K, V> actual constructor() : MutableMap<K, V> {
    private val backing = LinkedHashMap<K, V>()

    actual override val size: Int get() = backing.size
    actual override val keys: MutableSet<K> get() = backing.keys
    actual override val values: MutableCollection<V> get() = Values()
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = backing.entries

    actual override fun isEmpty(): Boolean = backing.isEmpty()
    actual override fun containsKey(key: K): Boolean = backing.containsKey(key)
    actual override fun containsValue(value: V): Boolean = backing.containsValue(value)
    actual override fun get(key: K): V? = backing[key]
    actual override fun put(key: K, value: V): V? = backing.put(key, value)
    actual override fun remove(key: K): V? = backing.remove(key)
    actual override fun putAll(from: Map<out K, V>) = backing.putAll(from)
    actual override fun clear() = backing.clear()

    /**
     * Live value view with JVM `remove` semantics. Kotlin/Native's `LinkedHashMap.values.remove`
     * scans from the newest entry and removes the last duplicate; `java.util.HashMap` removes the
     * first one in iteration order. The inherited [AbstractMutableCollection.remove] scans forward
     * through [iterator], which removes from [backing].
     */
    private inner class Values : AbstractMutableCollection<V>() {
        override val size: Int get() = backing.size
        override fun iterator(): MutableIterator<V> = backing.values.iterator()
        override fun contains(element: V): Boolean = backing.containsValue(element)
        override fun add(element: V): Boolean = throw UnsupportedOperationException()
        override fun clear() = backing.clear()
    }

    override fun equals(other: Any?): Boolean = this === other || backing == other
    override fun hashCode(): Int = backing.hashCode()
    override fun toString(): String = backing.toString()
}
