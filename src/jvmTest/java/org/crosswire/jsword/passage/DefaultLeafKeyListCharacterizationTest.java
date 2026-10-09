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
 * \u00A9 CrossWire Bible Society, 2026
 *
 */
package org.crosswire.jsword.passage;

import org.junit.Assert;
import org.junit.Test;

/**
 * Characterization of {@link DefaultLeafKeyList} with a null name. Java returned the null
 * from the getters; the Kotlin conversion declares them non-null and returns "" instead
 * (documented deviation) so that iterating such a key never throws.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class DefaultLeafKeyListCharacterizationTest {
    @Test
    public void iteratingListWithNullNamedLeafDoesNotThrow() {
        DefaultKeyList list = new DefaultKeyList(null, "list");
        DefaultLeafKeyList leaf = new DefaultLeafKeyList(null);
        list.addAll(leaf);
        int seen = 0;
        for (Key k : list) {
            Assert.assertSame(leaf, k);
            Assert.assertEquals("", k.getName());
            Assert.assertEquals("", k.getOsisRef());
            Assert.assertEquals("", k.getOsisID());
            Assert.assertEquals("", k.getRootName());
            seen++;
        }
        Assert.assertEquals(1, seen);
    }
}
