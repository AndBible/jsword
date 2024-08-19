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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.crosswire.common.util.Language;
import org.crosswire.common.util.PropertyMap;
import org.crosswire.common.util.ReflectionUtil;
import org.crosswire.common.util.ResourceUtil;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.index.lucene.LuceneIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory creating the appropriate Analyzer for natural language analysis of
 * text for Lucene Indexing and Query Parsing. Note: [Lang] refers to CommonName
 * for ISO639 Language Dependency: Analyzer from lucene contrib:
 * lucene-analyzers-[version].jar, lucene-smartcn-[version].jar,
 * lucene-snowball-[version].jar
 * 
 * Properties used: &lt;Key&gt; : &lt;Value&gt; Default.Analyzer : The default analyzer
 * class [Lang].Analyzer : Appropriate Analyzer class to be used for the
 * language of the book
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Sijo Cherian
 */
public final class AnalyzerFactory {
    public Analyzer createAnalyzer(Book book) {
        if (book == null) {
            return createAnalyzer((Language) null);
        } else {
            Analyzer analyzer = createAnalyzer(book.getLanguage());
            log.debug("{}: Using languageAnalyzer: {}", book.getBookMetaData().getInitials(), analyzer.getClass().getName());
            return analyzer;
        }
    }

    public Analyzer createAnalyzer(Language lang) {
        Analyzer analyzer = null;

        if (lang != null) {
            String aClass = getAnalyzerValue(lang);

            log.debug("Creating analyzer:{} BookLang:{}", aClass, lang);

            if (aClass != null) {
                try {
                    analyzer = ReflectionUtil.construct(aClass);
                } catch (ReflectiveOperationException e) {
                    log.error("Configuration error in AnalyzerFactory properties", e);
                }
            }
        }

        if (analyzer == null) {
            analyzer = new SimpleLuceneAnalyzer();
        }

        // Configure the analyzer
// The default analysis
        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();

        // Content is analyzed using natural language analyzer
        // (stemming, stopword etc)
        analyzerPerField.put(LuceneIndex.FIELD_BODY, analyzer);
        analyzerPerField.put(LuceneIndex.FIELD_BODY_STEM, analyzer);
        analyzerPerField.put(LuceneIndex.FIELD_INTRO_STEM, analyzer);
        analyzerPerField.put(LuceneIndex.FIELD_HEADING_STEM, analyzer);
        //analyzerPerField.put(LuceneIndex.FIELD_HEADING, myNaturalLanguageAnalyzer);  //heading to use same analyzer as BODY
        //analyzerPerField.put(LuceneIndex.FIELD_INTRO, myNaturalLanguageAnalyzer);

        // Keywords are normalized to osisIDs
        analyzerPerField.put(LuceneIndex.FIELD_KEY, new KeyAnalyzer());

        // Strong's Numbers are normalized to a consistent representation
        analyzerPerField.put(LuceneIndex.FIELD_STRONG, new StrongsNumberAnalyzer());

        // Strong's Numbers and Robinson's morphological codes are normalized to a consistent representation
        analyzerPerField.put(LuceneIndex.FIELD_MORPHOLOGY, new MorphologyAnalyzer());

        // XRefs are normalized from ranges into a list of osisIDs
        analyzerPerField.put(LuceneIndex.FIELD_XREF, new XRefAnalyzer());

        return new PerFieldAnalyzerWrapper(new SimpleAnalyzer(), analyzerPerField);
    }

    public static AnalyzerFactory getInstance() {
        return myInstance;
    }

    private AnalyzerFactory() {
        loadProperties();
    }

    public String getAnalyzerValue(Language lang) {
        String key = lang.getCode() + ".Analyzer";
        return myProperties.get(key);
    }

    private void loadProperties() {
        try {
            myProperties = ResourceUtil.getProperties(getClass());
        } catch (IOException e) {
            log.error("AnalyzerFactory property load from file failed", e);
        }
    }

    private static AnalyzerFactory myInstance = new AnalyzerFactory();

    private PropertyMap myProperties;

    /**
     * The log stream
     */
    private static final Logger log = LoggerFactory.getLogger(AnalyzerFactory.class);
}
