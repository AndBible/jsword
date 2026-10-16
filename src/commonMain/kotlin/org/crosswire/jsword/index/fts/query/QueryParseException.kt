package org.crosswire.jsword.index.fts.query

/**
 * The query is not valid Lucene classic query syntax (the baseline `org.apache.lucene.queryParser.ParseException`:
 * unbalanced quote or parenthesis, an operator without operand, an empty query, more than 1,024 clauses in one
 * boolean query, ...). `FtsIndex.find` wraps it in a `BookException("Search failed.")`, as the baseline does.
 */
internal class QueryParseException(message: String) : Exception(message)
