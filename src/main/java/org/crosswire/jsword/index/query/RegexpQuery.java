package org.crosswire.jsword.index.query;

import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.index.Index;
import org.crosswire.jsword.passage.Key;

public class RegexpQuery extends BaseQuery {
    /**
     * Construct a query from a string.
     *
     * @param theQuery
     */
    public RegexpQuery(String theQuery) {
        super(theQuery);
    }

    public Key find(Index index) throws BookException {
        return index.find(getQuery(), true);
    }

}
