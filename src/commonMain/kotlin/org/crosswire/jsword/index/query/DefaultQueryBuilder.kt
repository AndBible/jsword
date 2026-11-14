package org.crosswire.jsword.index.query

/**
 * The [QueryBuilder] of the JSword query layer: finds a range `+[...]`/`-[...]`/`[...]` and a blur ` ~N ` in the
 * request and builds the [Query] tree around them; everything else goes to the index as one [BaseQuery]. Port of the
 * former `LuceneQueryBuilder`, with the same strings and behaviour.
 */
class DefaultQueryBuilder : QueryBuilder {
    override fun parse(aQuery: String?): Query {
        var query: Query = NULL_QUERY

        var sought = aQuery
        if (sought == null || sought.isEmpty()) {
            return query
        }

        var range: Query? = null
        var rangeModifier = ""
        // Look for a range +[...], -[...], or [...]
        // A Lucene term range ("[a TO b]") is not a passage: it stays in the text, where the search parser reports it
        // as unsupported syntax instead of the whole query failing as an invalid passage (#4093).
        val rangeMatch = RANGE_PATTERN.findAll(sought).firstOrNull { !TERM_RANGE_PATTERN.matches(it.groupValues[2]) }
        if (rangeMatch != null) {
            rangeModifier = rangeMatch.groupValues[1]
            range = RangeQuery(rangeMatch.groupValues[2])
            sought = sought.replace(rangeMatch.value, " ")
        }

        // Look for a blur ~n
        val blurMatch = BLUR_PATTERN.find(sought)
        if (blurMatch != null) {
            // Did we have ~ or ~n?
            var blurFactor = 1
            val blur = blurMatch.groupValues[1]
            if (blur.isNotEmpty()) {
                blurFactor = blur.toInt()
            }
            val left = BaseQuery(sought.substring(0, blurMatch.range.first))
            val right = BaseQuery(sought.substring(blurMatch.range.last + 1))
            query = BlurQuery(left, right, blurFactor)
        } else if (sought.isNotEmpty()) {
            query = BaseQuery(sought)
        }

        if (range != null && NULL_QUERY != query) {
            query = if (rangeModifier.isEmpty() || rangeModifier[0] == '+') {
                AndQuery(range, query)
            } else {
                // AndNot needs to be after what it is restricting
                AndNotQuery(query, range)
            }
        }

        return query
    }

    private companion object {
        /**
         * The pattern of a range. This is anything that is contained between a
         * leading [] (but not containing a [ or ]), with a + or - optional prefix,
         * perhaps surrounded by whitespace.
         */
        private val RANGE_PATTERN = Regex("\\s*([-+]?)\\[([^\\[\\]]+)\\]\\s*")

        /** The content of a Lucene term range: two values around the upper-case `TO` keyword. */
        private val TERM_RANGE_PATTERN = Regex("\\s*\\S+\\s+TO\\s+\\S+\\s*")

        /** The pattern of a blur: a '~', optionally followed by a number, representing the number of verses. */
        private val BLUR_PATTERN = Regex("\\s~(\\d*)?\\s")

        /** A query that returns nothing. */
        private val NULL_QUERY: Query = NullQuery()
    }
}
