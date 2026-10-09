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
package org.crosswire.jsword.book.sword;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * Characterization of {@link DataEntry#getRawText(byte[])}: Java's String.trim() strips every
 * char up to U+0020 (including control characters such as U+001A), unlike Kotlin's trim().
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class DataEntryCharacterizationTest {
    @Test
    public void rawTextTrimsEveryCharUpToSpace() {
        byte[] data = " \u0001\u0002abc def\u001A \u0000".getBytes(StandardCharsets.UTF_8);
        DataEntry entry = new DataEntry("test", data, "UTF-8");
        Assert.assertEquals("abc def", entry.getRawText(null));
    }

    @Test
    public void rawTextKeepsNonAsciiWhitespace() {
        // NBSP (U+00A0) is > ' ', so Java's trim() keeps it
        byte[] data = "\u00A0abc\u00A0".getBytes(StandardCharsets.UTF_8);
        DataEntry entry = new DataEntry("test", data, "UTF-8");
        Assert.assertEquals("\u00A0abc\u00A0", entry.getRawText(null));
    }
}
