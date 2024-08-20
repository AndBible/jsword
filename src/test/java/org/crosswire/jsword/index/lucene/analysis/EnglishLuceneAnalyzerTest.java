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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.crosswire.common.util.Language;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the English Analyzer
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Sijo Cherian
 * @author DM Smith
 */
public class EnglishLuceneAnalyzerTest {

    @Before
    public void setUp() throws Exception {
        myAnalyzer = AnalyzerFactory.getInstance().createAnalyzer(new Language("en"));
        parser = new QueryParser(FIELD, myAnalyzer);
    }

    @Test
    public void testDefaultBehavior() throws ParseException {
        String testInput = "Surely will every man walketh";
        Query query = parser.parse(testInput);

        // stemming on
        Assert.assertTrue(query.toString().indexOf(FIELD + ":sure ") > -1);
        Assert.assertTrue(query.toString().indexOf(FIELD + ":everi") > -1);
    }

    @Test
    public void testStopwords() throws ParseException {
        Analyzer analyzer = AnalyzerFactory.getInstance().createAnalyzer(new Language("en"), true);
        parser = new QueryParser(FIELD, analyzer);
        String testInput = "Surely will every man walketh";
        Query query = parser.parse(testInput);

        // enable stop word
        Assert.assertTrue(query.toString().indexOf(FIELD + ":will") == -1);
    }

    protected static final String FIELD = "content";
    private Analyzer myAnalyzer;
    private QueryParser parser;
}
