package org.crosswire.common.util

/** Keeps the JVM map superclass while allowing inheritance on Native. */
expect open class PropertyMapSuperclass<K, V>() : MutableMap<K, V> {
    override val size: Int
    override val keys: MutableSet<K>
    override val values: MutableCollection<V>
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    override fun isEmpty(): Boolean
    override fun containsKey(key: K): Boolean
    override fun containsValue(value: V): Boolean
    override fun get(key: K): V?
    override fun put(key: K, value: V): V?
    override fun remove(key: K): V?
    override fun putAll(from: Map<out K, V>)
    override fun clear()
}
