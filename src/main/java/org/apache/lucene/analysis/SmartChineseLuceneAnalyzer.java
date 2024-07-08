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

import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;

/**
 * A simple wrapper for {@link SmartChineseAnalyzer}, which takes overlapping
 * two character tokenization approach which leads to larger index size, like
 * <code>org.apache.lucene.analyzer.cjk.CJKAnalyzer</code>. This analyzer's stop list
 * is merely of punctuation. It does stemming of English.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
final public class SmartChineseLuceneAnalyzer extends AbstractBookAnalyzer {
    public SmartChineseLuceneAnalyzer() {
        myAnalyzer = new SmartChineseAnalyzer();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return myAnalyzer.createComponents(fieldName);
    }

    private SmartChineseAnalyzer myAnalyzer;
}
