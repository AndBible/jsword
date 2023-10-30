/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.apache.lucene.analysis;

import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.crosswire.jsword.book.Book;

/**
 * A specialized analyzer that normalizes Cross References.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
final public class XRefAnalyzer extends AbstractBookAnalyzer {
    /**
     * Construct a default XRefAnalyzer.
     */
    public XRefAnalyzer() {
    }

    /**
     * Construct an XRefAnalyzer tied to a book.
     * 
     * @param book the book
     */
    public XRefAnalyzer(Book book) {
        setBook(book);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new KeyFilter(getBook(), source);
        return new TokenStreamComponents(source, result);
    }

}
