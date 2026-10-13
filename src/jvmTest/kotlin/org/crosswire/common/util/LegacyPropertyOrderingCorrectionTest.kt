package org.crosswire.common.util

import java.util.Properties
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Independent JDK authority for the production-used Native codec's enumeration and store order. */
class LegacyPropertyOrderingCorrectionTest {
    private val cases = listOf(
        listOf("z", "a", "m", "b"),
        listOf("Aa", "BB", "AaAa", "BBBB", "AaBB", "BBAa", "Aa", "BB"),
        (0..100).map { "key${100 - it}" } + listOf("key10", "key50", "key10"),
        // All 32 strings share a Java hash; this crosses resize and treeification thresholds.
        (0 until 32).map { bits -> (0 until 5).joinToString("") { bit -> if (bits and (1 shl bit) == 0) "Aa" else "BB" } }
    )

    @Test fun nativeLoadOrderMatchesPropertiesAcrossCollisionsDuplicateUpdatesAndGrowth() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        for (keys in cases) {
            val bytes = keys.mapIndexed { i, key -> "$key=value$i" }.joinToString("\n").toByteArray(Charsets.ISO_8859_1)
            val reference = Properties().apply { load(bytes.inputStream()) }
            assertEquals(reference.keys().toList(), LegacyPropertyText.entries(bytes).map { it.first }, "keys=$keys")
        }
    }

    @Test fun nativeStoreOrderMatchesPropertiesForExplicitInsertionHistory() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        for (keys in cases) {
            val map = linkedMapOf<String, String>()
            keys.forEachIndexed { i, key -> map[key] = "value$i" }
            val output = java.io.ByteArrayOutputStream()
            Properties().apply { putAll(map) }.store(output, null)
            fun body(bytes: ByteArray) = bytes.toString(Charsets.ISO_8859_1).lineSequence()
                .filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
            assertEquals(body(output.toByteArray()), body(LegacyPropertyText.bytes(map, null, "fixed")), "keys=$keys")
        }
    }

    @Test fun treeifiedEqualHashLoadHistoryMatchesRealProperties() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        val keys = cases.last()
        assertEquals(1, keys.map { it.hashCode() }.distinct().size)
        val input = keys.mapIndexed { i, key -> "$key=value$i" }.joinToString("\n").encodeToByteArray()
        val reference = Properties().apply { load(input.inputStream()) }
        assertEquals(reference.keys().toList(), LegacyPropertyText.entries(input).map { it.first })
    }

    @Test fun treeifiedEqualHashStoreHistoryMatchesRealProperties() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        val map = linkedMapOf<String, String>()
        cases.last().forEachIndexed { i, key -> map[key] = "value$i" }
        val output = java.io.ByteArrayOutputStream()
        Properties().apply { putAll(map) }.store(output, null)
        fun body(bytes: ByteArray) = bytes.toString(Charsets.ISO_8859_1).lineSequence()
            .filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
        assertEquals(body(output.toByteArray()), body(LegacyPropertyText.bytes(map, null, "fixed")))
    }

    @Test fun storePresizingDiffersFromIncrementalLoadAtCapacityBoundaries() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        for (count in listOf(0, 1, 7, 8, 11, 12, 15, 16, 23, 24, 47, 48, 95, 96)) {
            val map = linkedMapOf<String, String>()
            repeat(count) { map["boundary${count - it}"] = "value$it" }
            val input = map.entries.joinToString("\n") { "${it.key}=${it.value}" }.encodeToByteArray()
            val loaded = Properties().apply { load(input.inputStream()) }
            assertEquals(loaded.keys().toList(), LegacyPropertyText.entries(input).map { it.first }, "load count=$count")
            val reference = java.io.ByteArrayOutputStream()
            Properties().apply { putAll(map) }.store(reference, null)
            fun body(bytes: ByteArray) = bytes.toString(Charsets.ISO_8859_1).lineSequence()
                .filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
            assertEquals(body(reference.toByteArray()), body(LegacyPropertyText.bytes(map, null, "fixed")), "store count=$count")
        }
    }

    @Test fun mixedHashTreeSplitsRetainTraversalAndUntreeifyAtSixEntries() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        // At 24 distinct puts the default table is 64; the 48th put splits its tree bin.
        for ((lowCount, highCount) in listOf(6 to 7, 7 to 6, 7 to 7, 6 to 6, 9 to 0, 0 to 9)) {
            val low = collisionKeys(0, 20)
            val high = collisionKeys(64, 20)
            val filler = (0..10000).map { "filler$it" }.filter { spread(it) and 63 != 0 }.take(60)
            val keys = filler.take(24).toMutableList()
            repeat(maxOf(lowCount, highCount)) { index ->
                if (index < lowCount) keys += low[index]
                if (index < highCount) keys += high[index]
            }
            // Updates in a tree must neither change its first link nor count towards resize.
            keys += low.take(lowCount).reversed()
            keys += high.take(highCount).reversed()
            keys += filler.drop(24).take(47 - 24 - lowCount - highCount)
            assertLoadHistory(keys)
            keys += filler[50] // count 48: split with asymmetric six/seven-entry sides
            assertLoadHistory(keys)
            // List sides append; surviving tree sides prepend, then may treeify again.
            repeat(5) { index ->
                keys += low[lowCount + index]
                keys += high[highCount + index]
                keys += low[0]
                assertLoadHistory(keys)
            }
            keys += filler.drop(24)
            assertLoadHistory(keys)
        }
    }

    @Test fun mixedListResizeUsesLastRunRatherThanStablePartition() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        val low = collisionKeys(0, 4)
        val high = collisionKeys(16, 4)
        val filler = (0..1000).map { "ordinary$it" }.filter { spread(it) and 15 != 0 }.take(20)
        // All four collide at 16, alternate at 32, and remain below treeification.
        for (chain in listOf(listOf(low[0], high[0], low[1], high[1]),
            listOf(high[0], low[0], high[1], low[1]),
            listOf(low[0], high[0], low[1], low[2]))) {
            assertLoadHistory(chain + filler.take(8))
            assertLoadHistory(chain + filler + chain.reversed())
        }
    }

    @Test fun storePresizingAndCallerIterationMatchWithMixedTreeBins() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        val collisions = collisionKeys(0, 20).zip(collisionKeys(64, 20)).flatMap { listOf(it.first, it.second) }
        val fillers = (0..2000).map { "store$it" }.filter { spread(it) and 63 != 0 }.take(150)
        for (count in listOf(9, 15, 21, 22, 31, 42, 43, 47, 48, 95, 96, 150)) {
            val keys = (collisions.take(minOf(20, count)) + fillers).take(count)
            for (history in listOf(keys, keys.reversed(), keys.filterIndexed { i, _ -> i % 2 == 0 } +
                keys.filterIndexed { i, _ -> i % 2 == 1 })) {
                val map = linkedMapOf<String, String>()
                (history + history.take(10).reversed()).forEachIndexed { i, key -> map[key] = "value$i" }
                val output = java.io.ByteArrayOutputStream()
                Properties().apply { putAll(map) }.store(output, null)
                assertEquals(propertyBody(output.toByteArray()),
                    propertyBody(LegacyPropertyText.bytes(map, null, "fixed")), "count=$count history=$history")
            }
        }
    }

    private fun spread(key: String) = (key.hashCode() xor (key.hashCode() ushr 16)) and Int.MAX_VALUE

    private fun collisionKeys(bucket: Int, count: Int): List<String> =
        (0..100000).asSequence().map { "collision$it" }.filter { spread(it) and 127 == bucket }.take(count).toList()
            .also { assertEquals(count, it.size) }

    private fun assertLoadHistory(keys: List<String>) {
        val input = keys.mapIndexed { i, key -> "$key=value$i" }.joinToString("\n").encodeToByteArray()
        val reference = Properties().apply { load(input.inputStream()) }
        val actual = LegacyPropertyText.entries(input)
        assertEquals(reference.keys().toList(), actual.map { it.first }, "history=$keys")
        assertEquals(reference.entries.associate { it.key as String to it.value as String }, actual.toMap())
    }

    private fun propertyBody(bytes: ByteArray) = bytes.toString(Charsets.ISO_8859_1).lineSequence()
        .filter { it.isNotEmpty() && !it.startsWith('#') }.toList()

    @Test fun jdkTimestampCommentUsesDateToStringShapeAndDefaultTimezone() {
        assertEquals("17", System.getProperty("java.specification.version"), "Run with configured JDK17 authority")
        val output = java.io.ByteArrayOutputStream()
        Properties().store(output, null)
        val comment = output.toString("ISO-8859-1").lineSequence().first()
        assertTrue(Regex("#[A-Z][a-z]{2} [A-Z][a-z]{2} [ 0-9][0-9] [0-9]{2}:[0-9]{2}:[0-9]{2} .+ [0-9]{4}").matches(comment), comment)
    }
}
