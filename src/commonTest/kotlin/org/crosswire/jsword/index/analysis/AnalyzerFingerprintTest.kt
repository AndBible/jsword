package org.crosswire.jsword.index.analysis

import org.crosswire.jsword.index.fts.ANALYZER_OUTPUT_FINGERPRINT
import org.crosswire.jsword.index.fts.FTS_SCHEMA_VERSION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the rule documented at [FTS_SCHEMA_VERSION]: any change of analyzer output must bump the schema version, because
 * the stored `analyzerId` does not version the analyzer code. One short sample text per analyzer id (and one language
 * code that selects it) is analyzed and the whole output hashed; a mismatch means the output changed.
 */
class AnalyzerFingerprintTest {
    /** Language code to a sample text; every analyzer id of [BookAnalyzers] is covered (checked below). */
    private val samples: List<Pair<String, String>> = listOf(
        "en" to "In the beginning God created the heaven and the earth. Blessed are the poor, shouldn't they?",
        "he" to "בְּרֵאשִׁ֖ית בָּרָ֣א אֱלֹהִ֑ים אֵ֥ת הַשָּׁמַ֖יִם וְאֵ֥ת הָאָֽרֶץ",
        "ar" to "في البداية خلق الله السماوات والأرض",
        "fa" to "در ابتدا خدا آسمانها و زمین را آفرید",
        "th" to "ในปฐมกาลพระเจ้าทรงสร้างฟ้าสวรรค์และแผ่นดินโลก",
        "de" to "Am Anfang schuf Gott Himmel und Erde. Die Häuser der Männer",
        "cs" to "Na počátku stvořil Bůh nebe a zemi. Těžké časy",
        "el" to "Ἐν ἀρχῇ ἐποίησεν ὁ θεὸς τὸν οὐρανὸν καὶ τὴν γῆν",
        "zh" to "起初神创造天地。耶稣爱世人 God 123",
        "da" to "I begyndelsen skabte Gud himmelen og jorden",
        "es" to "En el principio creó Dios los cielos y la tierra",
        "fi" to "Alussa Jumala loi taivaan ja maan",
        "fr" to "Au commencement, Dieu créa les cieux et la terre",
        "it" to "In principio Dio creò il cielo e la terra",
        "nl" to "In het begin schiep God de hemel en de aarde",
        "no" to "I begynnelsen skapte Gud himmelen og jorden",
        "pt" to "No princípio Deus criou os céus e a terra",
        "ru" to "В начале Бог сотворил небо и землю",
        "sv" to "I begynnelsen skapade Gud himmel och jord",
    )

    private fun fnv1a64(s: String): String {
        var h = -3750763034362895579L // 0xcbf29ce484222325
        for (b in s.encodeToByteArray()) { h = (h xor (b.toLong() and 0xff)) * 1099511628211L }
        return h.toULong().toString(16).padStart(16, '0')
    }

    private fun outputOf(code: String, text: String): String =
        BookAnalyzers.analyzerId(code) + "|" + BookAnalyzers.forLanguage(code).analyze(text).joinToString(" ") { "${it.text}@${it.position}" }

    @Test fun theSampleCoversEveryAnalyzerId() {
        val covered = samples.map { BookAnalyzers.analyzerId(it.first) }.toSet()
        val all = listOf("simple", "he", "ar", "fa", "th", "de", "cs", "el", "mmseg") +
            listOf("Danish", "Spanish", "Finnish", "French", "Italian", "Dutch", "Norwegian", "Portuguese", "Russian", "Swedish").map { "snowball-$it" }
        assertEquals(all.toSet(), covered)
    }

    @Test fun analyzerOutputMatchesThePinnedFingerprint() {
        val canonical = samples.joinToString("\n") { (code, text) -> outputOf(code, text) }
        assertEquals(
            ANALYZER_OUTPUT_FINGERPRINT, fnv1a64(canonical),
            "The output of an analyzer changed. If that is intended, bump FTS_SCHEMA_VERSION (existing search indexes " +
                "must be rebuilt) and set ANALYZER_OUTPUT_FINGERPRINT in FtsSchema.kt to the new value. (schema version now $FTS_SCHEMA_VERSION)",
        )
    }

    /**
     * No analyzer gives two tokens the same position. This is why the query layer can reject a sloppy phrase whose terms
     * share a position (Lucene would build a MultiPhraseQuery) as Unsupported: it cannot occur (approved-differences.md).
     */
    @Test fun noAnalyzerEmitsTwoTokensAtOnePosition() {
        for ((code, text) in samples) {
            val positions = BookAnalyzers.forLanguage(code).analyze(text).map { it.position }
            assertTrue(positions.isNotEmpty(), code)
            assertEquals(positions.sorted().distinct(), positions, "positions not strictly increasing for $code: $positions")
        }
    }
}
