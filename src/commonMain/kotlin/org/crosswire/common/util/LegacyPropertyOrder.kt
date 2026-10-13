package org.crosswire.common.util

/**
 * The observable ordering of fresh JDK17 Properties for sequential String puts only.
 * Load starts with the default CHM capacity; store first presizes for putAll(map).
 * No concurrent access, removal, defaults, or general Map operations are modeled.
 *
 * CHM traverses TreeBin.first, not its red-black root. Rotations/recoloring never
 * change that list, so equality lookup and a tree/list flag suffice for ordering.
 */
internal class LegacyPropertyOrder {
    private class Entry(val hash: Int, val key: String, var value: String)
    private class Bin(val entries: MutableList<Entry>, var tree: Boolean = false)

    private var table = arrayOfNulls<Bin>(0)
    private var size = 0
    private var threshold = 16 // Properties() constructs CHM(8), whose initial sizeCtl is 16.

    fun put(key: String, value: String) {
        if (table.isEmpty()) allocate(threshold)
        val hash = spread(key)
        val index = hash and (table.size - 1)
        val bin = table[index]
        if (bin == null) {
            table[index] = Bin(mutableListOf(Entry(hash, key, value)))
        } else {
            val existing = bin.entries.indexOfFirst { it.hash == hash && it.key == key }
            val traversalCount = if (existing >= 0) existing + 1 else bin.entries.size
            if (existing >= 0) {
                bin.entries[existing].value = value
            } else if (bin.tree) {
                // putTreeVal prepends first; treeification itself retains the old list.
                bin.entries.add(0, Entry(hash, key, value))
            } else {
                bin.entries.add(Entry(hash, key, value))
            }
            // putVal calls treeifyBin even for a duplicate found sufficiently far down a list.
            if (!bin.tree && traversalCount >= 8) {
                if (table.size < 64) presize(table.size shl 1) else bin.tree = true
            }
            if (existing >= 0) return
        }
        size++
        while (size >= threshold && table.size < MAX_CAPACITY) resize()
    }

    fun entries(): List<Pair<String, String>> = buildList {
        table.forEach { bin -> bin?.entries?.forEach { add(it.key to it.value) } }
    }

    private fun allocate(capacity: Int) {
        table = arrayOfNulls(capacity)
        threshold = capacity - (capacity ushr 2)
    }

    private fun presize(count: Int) {
        val target = if (count >= MAX_CAPACITY / 2) MAX_CAPACITY
            else tableSizeFor(count + (count ushr 1) + 1)
        if (table.isEmpty()) allocate(maxOf(threshold, target))
        // tryPresize compares against sizeCtl AFTER allocation too, not just table length.
        while (target > threshold && table.size < MAX_CAPACITY) resize()
    }

    private fun resize() {
        val capacity = table.size
        val next = arrayOfNulls<Bin>(capacity shl 1)
        table.forEachIndexed { index, bin ->
            if (bin != null) {
                val low = mutableListOf<Entry>()
                val high = mutableListOf<Entry>()
                if (bin.tree) {
                    // Tree transfer preserves first-list order; sides of six or fewer untreeify.
                    bin.entries.forEach { entry ->
                        if (entry.hash and capacity == 0) low.add(entry) else high.add(entry)
                    }
                } else {
                    // CHM reuses the final same-bit run, prepending all earlier nodes per side.
                    // Stable partition (or reversing the whole chain) gives a different order.
                    var lastRun = 0
                    var lastBit = bin.entries[0].hash and capacity
                    for (position in 1 until bin.entries.size) {
                        val bit = bin.entries[position].hash and capacity
                        if (bit != lastBit) {
                            lastBit = bit
                            lastRun = position
                        }
                    }
                    for (position in lastRun - 1 downTo 0) {
                        val entry = bin.entries[position]
                        if (entry.hash and capacity == 0) low.add(entry) else high.add(entry)
                    }
                    val tail = if (lastBit == 0) low else high
                    for (position in lastRun until bin.entries.size) tail.add(bin.entries[position])
                }
                if (low.isNotEmpty()) next[index] = Bin(low, bin.tree && low.size > 6)
                if (high.isNotEmpty()) next[index + capacity] = Bin(high, bin.tree && high.size > 6)
            }
        }
        table = next
        threshold = next.size - (next.size ushr 2)
    }

    companion object {
        private const val MAX_CAPACITY = 1 shl 30

        fun storeEntries(map: Map<String, String>): List<Pair<String, String>> =
            LegacyPropertyOrder().apply {
                presize(map.size)
                map.forEach { (key, value) -> put(key, value) }
            }.entries()

        private fun spread(key: String): Int {
            var hash = 0
            key.forEach { hash = 31 * hash + it.code } // Java String hash over UTF-16 code units.
            return (hash xor (hash ushr 16)) and Int.MAX_VALUE
        }

        private fun tableSizeFor(size: Int): Int {
            var capacity = 1
            while (capacity < size && capacity < MAX_CAPACITY) capacity = capacity shl 1
            return capacity
        }
    }
}
