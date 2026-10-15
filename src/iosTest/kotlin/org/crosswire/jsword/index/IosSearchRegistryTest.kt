package org.crosswire.jsword.index

import org.crosswire.jsword.index.query.QueryBuilderFactory
import org.crosswire.jsword.index.query.QueryDecoratorFactory
import org.crosswire.jsword.index.search.SearchType
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IosSearchRegistryTest {
    @Test fun iosRegistryHasUnsupportedDefaults() {
        assertTrue(IndexManagerFactory.getIndexManager() is UnsupportedIndexManager)
        assertTrue(QueryDecoratorFactory.getSearchSyntax() is org.crosswire.jsword.index.query.UnsupportedQueryDecorator)
        assertFailsWith<SearchUnsupportedException> { QueryBuilderFactory.getQueryBuilder() }
    }

    @Test fun searchTypeClassInitialisesAndFailsOnUse() {
        val t = SearchType.PHRASE   // class init must not throw
        assertFailsWith<SearchUnsupportedException> { t.decorate("in the beginning") }
    }
}
