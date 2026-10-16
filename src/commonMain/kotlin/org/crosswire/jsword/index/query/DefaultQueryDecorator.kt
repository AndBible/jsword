package org.crosswire.jsword.index.query

/**
 * The [QueryDecorator] producing the JSword query syntax the FTS5 search understands (the Lucene classic query
 * syntax subset): `+a +b`, `"a b"`, `-a -b`, `a* b*`, `a~ b~`, `+[range]`. Port of the former `LuceneQueryDecorator`.
 */
class DefaultQueryDecorator : QueryDecorator {
    override fun decorateAllWords(queryWords: String?): String {
        val words = words(queryWords!!)
        return PLUS + words.joinToString(SPACE_PLUS)
    }

    /** Nothing to do: any word is the default behaviour. */
    override fun decorateAnyWords(queryWords: String?): String = queryWords!!

    override fun decoratePhrase(queryWords: String?): String = QUOTE + queryWords!! + QUOTE

    override fun decorateNotWords(queryWords: String?): String {
        val words = words(queryWords!!)
        return MINUS + words.joinToString(SPACE_MINUS)
    }

    override fun decorateRange(queryWords: String?): String = PLUS + OPEN + queryWords!! + CLOSE

    override fun decorateSpellWords(queryWords: String?): String {
        val words = words(queryWords!!)
        return words.joinToString(FUZZY_SPACE) + FUZZY
    }

    override fun decorateStartWords(queryWords: String?): String {
        val words = words(queryWords!!)
        return words.joinToString(WILD_SPACE) + WILD
    }

    /** Java's `String.split(" ")` semantics: trailing empty strings are dropped (the original used it). */
    private fun words(queryWords: String): List<String> = queryWords.split(SPACE).dropLastWhile { it.isEmpty() }

    private companion object {
        private const val SPACE = " "
        private const val QUOTE = "\""
        private const val PLUS = "+"
        private const val SPACE_PLUS = " +"
        private const val MINUS = "-"
        private const val SPACE_MINUS = " -"
        private const val OPEN = "["
        private const val CLOSE = "]"
        private const val FUZZY = "~"
        private const val FUZZY_SPACE = "~ "
        private const val WILD = "*"
        private const val WILD_SPACE = "* "
    }
}
