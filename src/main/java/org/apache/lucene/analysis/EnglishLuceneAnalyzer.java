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

import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;

/**
 * English Analyzer works like lucene SimpleAnalyzer + Stemming.
 * (LowerCaseTokenizer &gt; PorterStemFilter). Like the AbstractAnalyzer,
 * {@link StopFilter} is off by default.
 * 
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author sijo cherian
 */
final public class EnglishLuceneAnalyzer extends AbstractBookAnalyzer {

    public EnglishLuceneAnalyzer() {
        stopSet = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
    }


    /**
     * Constructs a {@link LetterTokenizer} with {@link LowerCaseFilter} filtered by a language filter
     * {@link StopFilter} and {@link PorterStemFilter} for English.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new LetterTokenizer();
        TokenStream result = new LowerCaseFilter(source);

        if (doStopWords && stopSet != null) {
            result = new StopFilter(result, (CharArraySet) stopSet);
        }

        // Using Porter Stemmer
        if (doStemming) {
            result = new PorterStemFilter(result);
        }

        return new TokenStreamComponents(source, result);
    }

}
