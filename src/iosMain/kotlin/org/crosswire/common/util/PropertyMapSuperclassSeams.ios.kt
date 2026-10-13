package org.crosswire.common.util

actual open class PropertyMapSuperclass<K, V> actual constructor() : MutableMap<K, V> {
    private val backing = LinkedHashMap<K, V>()

    actual override val size: Int get() = backing.size
    actual override val keys: MutableSet<K> get() = backing.keys
    actual override val values: MutableCollection<V> get() = backing.values
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = backing.entries

    actual override fun isEmpty(): Boolean = backing.isEmpty()
    actual override fun containsKey(key: K): Boolean = backing.containsKey(key)
    actual override fun containsValue(value: V): Boolean = backing.containsValue(value)
    actual override fun get(key: K): V? = backing[key]
    actual override fun put(key: K, value: V): V? = backing.put(key, value)
    actual override fun remove(key: K): V? = backing.remove(key)
    actual override fun putAll(from: Map<out K, V>) = backing.putAll(from)
    actual override fun clear() = backing.clear()

    override fun equals(other: Any?): Boolean = this === other || backing == other
    override fun hashCode(): Int = backing.hashCode()
    override fun toString(): String = backing.toString()
}
