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
package org.crosswire.jsword.index.lucene.analysis;

import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.crosswire.common.util.Language;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.basic.AbstractBook;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author sijo cherian
 * @author DM Smith
 */
public class AnalyzerFactoryTest {

    /**
     * Test method for
     * {@link AnalyzerFactory#createAnalyzer(org.crosswire.jsword.book.Book)}
     * .
     */
    @Test
    public void testCreateAnalyzer() {
        Analyzer myAnalyzer = AnalyzerFactory.getInstance().createAnalyzer(new Language("en"));
        Assert.assertTrue(myAnalyzer != null);

        myAnalyzer = AnalyzerFactory.getInstance().createAnalyzer((Book) null);
        Assert.assertTrue(myAnalyzer != null);
    }

    @Test
    public void testDiacriticFiltering() throws Exception {
        Analyzer myAnalyzer = AnalyzerFactory.getInstance().createAnalyzer(new Language("en"));
        QueryParser parser = new QueryParser(FIELD, myAnalyzer);
        String testInput = "Surely will every man walketh";

        Query query = parser.parse(testInput);

        Assert.assertTrue(query.toString().contains(FIELD + ":sure "));
        Assert.assertTrue(query.toString().contains(FIELD + ":everi"));
    }

    @Test
    public void testStopWordsFiltering() throws Exception {
        Analyzer myAnalyzer = AnalyzerFactory.getInstance().createAnalyzer(new Language("en"));
        QueryParser parser = new QueryParser(FIELD, myAnalyzer);
        String testInput = "Surely will every man walketh";
        // enable stop words
        Query query = parser.parse(testInput);
        System.out.println(query.toString());
        Assert.assertTrue(!query.toString().contains(FIELD + ":will"));
    }

    /*
     * public void testLatin1Language() throws ParseException { Analyzer
     * myAnalyzer = AnalyzerFactory.getInstance().createAnalyzer("Latin");
     *
     * 
     * QueryParser parser = new QueryParser(field, myAnalyzer);
     * 
     * String testInput = "test \u00D9\u00EB\u0153";
     * Assert.assertTrue(myAnalyzer instanceof SimpleLuceneAnalyzer); Query query =
     * parser.parse(testInput); //After Diacritic filtering
     * Assert.assertTrue(query.toString().indexOf(field+":ueoe") > -1);
     * 
     * testInput = "A\u00C1"; query = parser.parse(testInput);
     * //After Diacritic filtering
     * Assert.assertTrue(query.toString().indexOf(field+":aa") > -1);
     * 
     * 
     * }
     */
    protected static final String FIELD = "content";
}
