package org.crosswire.jsword.index.analysis.snowball

/**
 * A new Snowball stemmer for [name], one of the ten languages the baseline `ConfigurableSnowballAnalyzer` is used
 * with here: `Danish`, `Dutch`, `Finnish`, `French`, `Italian`, `Norwegian`, `Portuguese`, `Russian`, `Spanish`,
 * `Swedish` (the part of the Lucene class name before `Stemmer`; case-sensitive). Null for any other name.
 *
 * Each call returns a fresh instance; a stemmer is not thread-safe.
 */
internal fun snowballStemmer(name: String): SnowballStemmer? = when (name) {
    "Danish" -> DanishStemmer()
    "Dutch" -> DutchStemmer()
    "Finnish" -> FinnishStemmer()
    "French" -> FrenchStemmer()
    "Italian" -> ItalianStemmer()
    "Norwegian" -> NorwegianStemmer()
    "Portuguese" -> PortugueseStemmer()
    "Russian" -> RussianStemmer()
    "Spanish" -> SpanishStemmer()
    "Swedish" -> SwedishStemmer()
    else -> null
}
