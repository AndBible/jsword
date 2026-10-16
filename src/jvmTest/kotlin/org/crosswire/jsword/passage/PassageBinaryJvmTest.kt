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
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.passage;

import org.crosswire.jsword.book.CaseType;
import org.crosswire.jsword.versification.BookName;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Test.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
public class PassageBinaryJvmTest {
    @Test
    public void testBinary() {
        byte[] buffer;
        int[] index;

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(1, PassageKeyFactory.toBinary(buffer, 0, 0, 0));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 0));
        Assert.assertEquals(1, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(1, PassageKeyFactory.toBinary(buffer, 0, 0, 1));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 1));
        Assert.assertEquals(1, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(1, PassageKeyFactory.toBinary(buffer, 0, 0, 255));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 255));
        Assert.assertEquals(1, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(2, PassageKeyFactory.toBinary(buffer, 0, 0, 256));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 256));
        Assert.assertEquals(2, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(2, PassageKeyFactory.toBinary(buffer, 0, 0, 65535));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 65535));
        Assert.assertEquals(2, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(3, PassageKeyFactory.toBinary(buffer, 0, 0, 65536));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 65536));
        Assert.assertEquals(3, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(3, PassageKeyFactory.toBinary(buffer, 0, 0, 16777215));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 16777215));
        Assert.assertEquals(3, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(4, PassageKeyFactory.toBinary(buffer, 0, 0, 16777216));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 16777216));
        Assert.assertEquals(4, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(4, PassageKeyFactory.toBinary(buffer, 0, 0, 2147483647));
        Assert.assertEquals(0, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
        Assert.assertEquals(0, PassageKeyFactory.fromBinary(buffer, index, 2147483647));
        Assert.assertEquals(4, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
        };
        Assert.assertEquals(1, PassageKeyFactory.toBinary(buffer, 0, 1, 1));
        Assert.assertEquals(1, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
        Assert.assertEquals(1, PassageKeyFactory.fromBinary(buffer, index, 1));
        Assert.assertEquals(1, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
        };
        Assert.assertEquals(1, PassageKeyFactory.toBinary(buffer, 0, 255, 255));
        Assert.assertEquals(-1, buffer[0]);
        Assert.assertEquals(0, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
        Assert.assertEquals(255, PassageKeyFactory.fromBinary(buffer, index, 255));
        Assert.assertEquals(1, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
        };
        Assert.assertEquals(2, PassageKeyFactory.toBinary(buffer, 0, 65535, 65535));
        Assert.assertEquals(-1, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(0, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
        Assert.assertEquals(65535, PassageKeyFactory.fromBinary(buffer, index, 65535));
        Assert.assertEquals(2, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
        };
        Assert.assertEquals(3, PassageKeyFactory.toBinary(buffer, 0, 16777215, 16777215));
        Assert.assertEquals(-1, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(0, buffer[3]);
        Assert.assertEquals(16777215, PassageKeyFactory.fromBinary(buffer, index, 16777215));
        Assert.assertEquals(3, index[0]);

        index = new int[] {
            0
        };
        buffer = new byte[] {
                (byte) 0, (byte) 0, (byte) 0, (byte) 0,
        };
        Assert.assertEquals(4, PassageKeyFactory.toBinary(buffer, 0, 2147483647, 2147483647));
        Assert.assertEquals(127, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(-1, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(2147483647, PassageKeyFactory.fromBinary(buffer, index, 2147483647));
        Assert.assertEquals(4, index[0]);

        index = new int[] {
            2
        };
        buffer = new byte[] {
                (byte) -1, (byte) -1, (byte) -1, (byte) -1,
        };
        Assert.assertEquals(1, PassageKeyFactory.toBinary(buffer, 2, 10, 11));
        Assert.assertEquals(-1, buffer[0]);
        Assert.assertEquals(-1, buffer[1]);
        Assert.assertEquals(10, buffer[2]);
        Assert.assertEquals(-1, buffer[3]);
        Assert.assertEquals(10, PassageKeyFactory.fromBinary(buffer, index, 11));
        Assert.assertEquals(3, index[0]);

        for (int i = 0; i < 2147400000; i += 10000) {
            index = new int[] {
                0
            };
            buffer = new byte[] {
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            };
            Assert.assertEquals(4, PassageKeyFactory.toBinary(buffer, 0, i, 2147483647));
            Assert.assertEquals(i, PassageKeyFactory.fromBinary(buffer, index, 2147483647));
            Assert.assertEquals(4, index[0]);
        }
    }

}
