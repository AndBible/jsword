package org.crosswire.jsword.index.analysis

import org.crosswire.common.util.Language
import org.crosswire.jsword.book.Book

/**
 * Selects the analyzer for a book language. The table is the baseline `AnalyzerFactory.properties`, keyed by the
 * language code that `Language` resolves (so `zh-Hans`, `zh-Hant` are `zh`, and `pt-BR` is `pt`). A code without an
 * entry, including `hbo` (ancient Hebrew, unmapped in the baseline), `en` and a missing language, gets the simple
 * analyzer, as the baseline `AnalyzerFactory.createAnalyzer` falls back to `SimpleLuceneAnalyzer`.
 *
 * Analyzers are cached per analyzer id and shared by all callers. This is safe because no analyzer keeps state
 * between [TextAnalyzer.analyze] calls: the tokenizers and the mmseg dictionary hold only immutable tables, and the
 * stemmers are created per call. Each instance is created on first use (the mmseg dictionary is large).
 */
object BookAnalyzers {
    /** Language code to analyzer id, mirroring `AnalyzerFactory.properties` (Default is `simple`). */
    private val ids: Map<String, String> = mapOf(
        "ar" to "ar",
        "cs" to "cs",
        "de" to "de",
        "el" to "el",
        "grc" to "el",
        "fa" to "fa",
        "he" to "he",
        "ja" to "mmseg",
        "zh" to "mmseg",
        "th" to "th",
        "da" to "snowball-Danish",
        "es" to "snowball-Spanish",
        "fi" to "snowball-Finnish",
        "fr" to "snowball-French",
        "it" to "snowball-Italian",
        "nl" to "snowball-Dutch",
        "no" to "snowball-Norwegian",
        "pt" to "snowball-Portuguese",
        "ru" to "snowball-Russian",
        "sv" to "snowball-Swedish",
    )

    private const val SIMPLE = "simple"

    private val instances: Map<String, Lazy<TextAnalyzer>> =
        (ids.values + SIMPLE).distinct().associateWith { id -> lazy { create(id) } }

    /**
     * The id of the analyzer for [code], one of `simple`, `ar`, `cs`, `de`, `el`, `fa`, `he`, `mmseg`, `th`, `snowball-<Name>`.
     * A specification with a region or script subtag (`zh-Hans`, `pt-BR`) is first reduced to its language code by
     * [Language], as the baseline does, so it selects the analyzer of that code.
     */
    fun analyzerId(code: String?): String {
        val key = if (code != null && code.contains('-')) Language(code).code else code
        return key?.let { ids[it] } ?: SIMPLE
    }

    /** The shared analyzer for [code] (see the class comment). */
    fun forLanguage(code: String?): TextAnalyzer = instances.getValue(analyzerId(code)).value

    /** The shared analyzer for the language of [book]; a book without a language gets the simple analyzer. */
    fun forBook(book: Book): TextAnalyzer = forLanguage(book.language.code)

    private fun create(id: String): TextAnalyzer = when (id) {
        SIMPLE -> SimpleAnalyzer()
        "ar" -> ArabicAnalyzer()
        "cs" -> CzechAnalyzer()
        "de" -> GermanAnalyzer()
        "el" -> GreekAnalyzer()
        "fa" -> PersianAnalyzer()
        "he" -> HebrewAnalyzer()
        "mmseg" -> MmsegAnalyzer()
        "th" -> ThaiAnalyzer()
        else -> SnowballAnalyzer(id.removePrefix("snowball-"))
    }
}
