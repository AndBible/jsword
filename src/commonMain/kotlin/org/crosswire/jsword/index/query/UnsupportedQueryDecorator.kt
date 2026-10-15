package org.crosswire.jsword.index.query

import org.crosswire.jsword.index.SearchUnsupportedException

/** iOS default until K6: `SearchType` initialises, every decoration is refused. */
internal class UnsupportedQueryDecorator : QueryDecorator {
    override fun decoratePhrase(queryWords: String?): String = throw SearchUnsupportedException()
    override fun decorateAllWords(queryWords: String?): String = throw SearchUnsupportedException()
    override fun decorateAnyWords(queryWords: String?): String = throw SearchUnsupportedException()
    override fun decorateNotWords(queryWords: String?): String = throw SearchUnsupportedException()
    override fun decorateStartWords(queryWords: String?): String = throw SearchUnsupportedException()
    override fun decorateSpellWords(queryWords: String?): String = throw SearchUnsupportedException()
    override fun decorateRange(queryWords: String?): String = throw SearchUnsupportedException()
}
