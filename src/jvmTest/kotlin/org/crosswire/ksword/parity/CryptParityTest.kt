package org.crosswire.ksword.parity

import kotlin.test.Test
import kotlin.test.assertEquals

/** Sapphire: current vs baseline on keys and data that exercise every branch (K0 pilot). */
class CryptParityTest {
    private val plain = "00010203fffefd7f80" + "48656c6c6f2c20576f726c6421"

    private val keys = listOf(
        "<null>",                       // Java accepted a null key (hash mode)
        "",                             // empty key (hash mode)
        "simple",                       // passes through personalize() unchanged
        "ABCD-EFGH-IJKL-MNOP-QRST",     // license-shaped; wrong checksum → unchanged
        "Ünïcödé-Ω-漢字",                // chars above U+00FF become '?' in the probe's getBytes(ISO_8859_1), identically on both sides, before Sapphire sees them (exercises the probe's encoding path, not Sapphire's own latin1 helper)
        "test-VvUj-UeY2-Yg11-nKAK",     // valid license key: personalize() transform branch
    )

    @Test
    fun cipherMatchesBaseline() {
        for (key in keys) {
            assertEquals(Oracle.call("sapphire.cipher", key, plain), Current.call("sapphire.cipher", key, plain), "key=$key")
        }
    }

    @Test
    fun hashMatchesBaseline() {
        for (key in keys.filter { it != "<null>" }) {
            for (length in listOf(1, 20, 64)) {
                assertEquals(
                    Oracle.call("sapphire.hash", key, plain, "$length"),
                    Current.call("sapphire.hash", key, plain, "$length"),
                    "key=$key length=$length",
                )
            }
        }
    }
}
