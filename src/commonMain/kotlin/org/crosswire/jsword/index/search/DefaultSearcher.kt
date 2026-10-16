package org.crosswire.jsword.index.search

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.index.Index
import org.crosswire.jsword.index.query.Query
import org.crosswire.jsword.index.query.QueryBuilderFactory
import org.crosswire.jsword.passage.Key

/** The [Searcher] over an [Index]: builds the query tree with the [QueryBuilderFactory] and runs it. Port of `LuceneSearcher`. */
class DefaultSearcher : Searcher {
    private var index: Index? = null

    override fun init(index: Index?) {
        this.index = index
    }

    @Throws(BookException::class)
    fun search(request: String): Key = search(DefaultSearchRequest(request))

    @Throws(BookException::class)
    override fun search(request: SearchRequest?): Key {
        val idx = index!!
        idx.searchModifier = request!!.searchModifier
        val query = QueryBuilderFactory.getQueryBuilder().parse(request.request)
        val results = search(query)
        idx.searchModifier = null
        return results
    }

    @Throws(BookException::class)
    override fun search(request: Query?): Key = request!!.find(index)
}
