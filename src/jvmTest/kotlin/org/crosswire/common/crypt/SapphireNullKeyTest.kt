package org.crosswire.common.crypt

import kotlin.test.Test
import kotlin.test.assertContentEquals

/** Java callers may pass a null or empty key (hash mode); the Kotlin constructor must keep accepting both. */
class SapphireNullKeyTest {
    private fun stream(s: Sapphire): ByteArray = ByteArray(32) { s.cipher(it.toByte()) }

    @Test
    fun nullAndEmptyKeyBehaveIdentically() {
        val nullKey = stream(Sapphire(null))
        val emptyKey = stream(Sapphire(ByteArray(0)))
        assertContentEquals(nullKey, emptyKey)
    }

    @Test
    fun hashModeIsDeterministicAndKeyedModeDiffers() {
        val a = ByteArray(16).also { Sapphire(null).hashFinal(it) }
        val b = ByteArray(16).also { Sapphire(ByteArray(0)).hashFinal(it) }
        assertContentEquals(a, b)
        val keyed = ByteArray(16).also { Sapphire("k".toByteArray()).hashFinal(it) }
        check(!keyed.contentEquals(a)) { "keyed hash must differ from unkeyed" }
    }
}
