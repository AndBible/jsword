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

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.crosswire.jsword.JSMsg;
import org.crosswire.jsword.book.study.StrongsNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A StrongsNumberFilter normalizes Strong's Numbers.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
public class StrongsNumberFilter extends FilteringTokenFilter {

    /**
     * Construct filtering <i>in</i>.
     * 
     * @param in 
     */
    public StrongsNumberFilter(TokenStream in) {
        super(in);
        this.termAtt = addAttribute(CharTermAttribute.class);
    }

    @Override
    public boolean accept() {
        boolean valid;
        String tokenText = termAtt.toString();
        if (number == null) {
            number = new StrongsNumber(tokenText);
            valid = number.isValid();
            termAtt.setEmpty().append(number.getStrongsNumber());
            if (!number.isPart()) {
                number = null;
            }
        } else {
            valid = number.isValid();
            termAtt.setEmpty().append(number.getFullStrongsNumber());
            number = null;
        }
        if (!valid) {
            log.warn(JSMsg.gettext("Not a valid Strong's Number \"{0}\"", tokenText));
        }
        return valid;
    }

    private CharTermAttribute termAtt;
    private StrongsNumber number;

    /**
     * The log stream
     */
    private static final Logger log = LoggerFactory.getLogger(StrongsNumberFilter.class);
}
