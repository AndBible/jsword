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
 * © CrossWire Bible Society, 2009 - 2016
 *
 */
package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
import org.apache.lucene.analysis.ar.ArabicStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

/**
 * An Analyzer whose {@link TokenStream} is built from a
 * {@link StandardTokenizer} filtered with {@link LowerCaseFilter},
 * {@link ArabicNormalizationFilter}, {@link ArabicStemFilter} (optional) and
 * Arabic {@link StopFilter} (optional).
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
final public class ArabicLuceneAnalyzer extends AbstractBookAnalyzer {
    public ArabicLuceneAnalyzer() {
        stopSet = ArabicAnalyzer.getDefaultStopSet();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new ArabicNormalizationFilter(result);
        if (doStopWords && stopSet != null) {
            result = new StopFilter(result, (CharArraySet) stopSet);
        }

        if (doStemming) {
            result = new ArabicStemFilter(result);
        }

        return new TokenStreamComponents(source, result);
    }

}
