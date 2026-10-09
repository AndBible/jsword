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

import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;
import org.junit.Assert;
import org.junit.Test;

/**
 * Characterization of reference parsing in AbstractPassage.addVerses:
 * the parts split at ';' are trimmed with Java's String.trim() (chars up to U+0020 only). The reference
 * parser ignores such characters anyway, so these cases pin the observable result (verified: the result is
 * the same with Kotlin's trim()), they cannot detect a trim() regression by themselves.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class PassageKeyFactoryCharacterizationTest {
    private final Versification v11n = Versifications.instance().getVersification("KJV");

    @Test
    public void controlCharsAndSpacesAfterSemicolonAreTrimmed() throws Exception {
        Passage p = PassageKeyFactory.instance().getKey(v11n, "Gen 1:1;\u0001 Gen 1:2");
        Assert.assertEquals(2, p.countVerses());
    }

    @Test
    public void nbspAfterSemicolonStillParsesInSingleAttemptParser() throws Exception {
        // PassageKeyFactory.getKey would retry with a normalized reference and hide differences, so the
        // single-attempt parser (AbstractPassage.addVerses, via the RangedPassage constructor) is used directly.
        // U+00A0 is not trimmed by Java's String.trim(); the reference parser itself copes with it.
        Passage p = new RangedPassage(v11n, "Gen 1:1;\u00A0Gen 1:2", null);
        Assert.assertEquals(2, p.countVerses());
    }

    @Test
    public void controlCharsAreTrimmedInSingleAttemptParser() throws Exception {
        Passage p = new RangedPassage(v11n, "Gen 1:1;\u0001 Gen 1:2", null);
        Assert.assertEquals(2, p.countVerses());
    }
}
