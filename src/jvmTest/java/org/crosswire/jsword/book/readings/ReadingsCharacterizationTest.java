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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.readings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.book.BookDriver;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.FeatureType;
import org.crosswire.jsword.passage.DefaultLeafKeyList;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.PreferredKey;
import org.jdom2.Content;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Characterization of {@link ReadingsBookDriver}, {@link ReadingsBook} and
 * {@link ReadingsKey}: pins the behaviour of the Java classes before they are
 * converted to Kotlin. Expected values were derived by reading the Java and the
 * built-in reading set ot1nt2.properties. The date dependent values are computed
 * from the current year, never hard coded.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class ReadingsCharacterizationTest {
    private ReadingsBookDriver driver;
    private Book book;

    @Before
    public void setUp() {
        driver = new ReadingsBookDriver();
        book = driver.getBooks()[0];
    }

    private static Date date(int year, int month, int day) {
        // Noon, to stay away from any midnight boundary
        return new GregorianCalendar(year, month, day, 12, 0, 0).getTime();
    }

    private static String mediumName(Date date) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
    }

    @Test
    public void testDriver() {
        Assert.assertEquals("Readings", driver.getDriverName());
        Assert.assertFalse(driver.isWritable());
        Assert.assertEquals("readings", ReadingsBookDriver.DIR_READINGS);
        // readings.txt lists a single set
        Assert.assertArrayEquals(new String[] {"ot1nt2"}, driver.getInstalledReadingsSets());
        Assert.assertEquals(1, driver.getBooks().length);
        // getBooks hands out a copy of the array
        Assert.assertNotSame(driver.getBooks(), driver.getBooks());
        Assert.assertSame(driver.getBooks()[0], driver.getBooks()[0]);
        BookDriver shared = ReadingsBookDriver.instance();
        Assert.assertNotNull(shared);
        Assert.assertSame(shared, ReadingsBookDriver.instance());
        Assert.assertSame(shared, shared.getBooks()[0].getDriver());
    }

    @Test
    public void testBookMetaData() {
        Assert.assertEquals("ot1nt2", book.getInitials());
        Assert.assertEquals("Read OT Once and NT Twice per Year in 3 portions", book.getName());
        Assert.assertEquals(BookCategory.DAILY_DEVOTIONS, book.getBookCategory());
        Assert.assertEquals("en", book.getLanguage().getCode());
        Assert.assertSame(driver, book.getDriver());
        Assert.assertTrue(book.hasFeature(FeatureType.DAILY_DEVOTIONS));
        Assert.assertFalse(book.hasFeature(FeatureType.STRONGS_NUMBERS));
        Assert.assertFalse(book.isWritable());
    }

    @Test
    public void testGlobalKeyListHasOneKeyPerDayOfTheCurrentYear() {
        Key global = book.getGlobalKeyList();
        int daysInYear = new GregorianCalendar().getActualMaximum(Calendar.DAY_OF_YEAR);
        // The set has 366 entries (it includes 29 February); only the days of this year are used.
        Assert.assertEquals(daysInYear, global.getCardinality());
        Assert.assertSame(global, book.getGlobalKeyList());
        Assert.assertEquals(global.getName(), book.getName());

        int year = new GregorianCalendar().get(Calendar.YEAR);
        Key first = global.get(0);
        Assert.assertTrue(first instanceof ReadingsKey);
        Assert.assertEquals(mediumName(date(year, Calendar.JANUARY, 1)), first.getName());
        Assert.assertEquals("1.January", first.getOsisRef().replace("\u00A0", " "));
        Key last = global.get(daysInYear - 1);
        Assert.assertEquals(mediumName(date(year, Calendar.DECEMBER, 31)), last.getName());
    }

    @Test
    public void testExternal2Internal() {
        Assert.assertEquals("01.01", ReadingsKey.external2internal(new GregorianCalendar(2026, Calendar.JANUARY, 1)));
        Assert.assertEquals("02.29", ReadingsKey.external2internal(new GregorianCalendar(2028, Calendar.FEBRUARY, 29)));
        Assert.assertEquals("12.05", ReadingsKey.external2internal(new GregorianCalendar(2026, Calendar.DECEMBER, 5)));
        Assert.assertEquals("10.31", ReadingsKey.external2internal(new GregorianCalendar(2026, Calendar.OCTOBER, 31)));
    }

    @Test
    public void testGetKeyParsesALenientDate() throws Exception {
        for (Date d : new Date[] {date(2026, Calendar.JANUARY, 1), date(2028, Calendar.FEBRUARY, 29)}) {
            String text = mediumName(d);
            Key reply = book.getKey(text);
            Assert.assertEquals(1, reply.getCardinality());
            Key inner = reply.get(0);
            Assert.assertTrue(inner instanceof ReadingsKey);
            Assert.assertEquals(text, inner.getName());
            // The key remembers the date it was parsed from: midnight of that day, i.e. before noon
            Assert.assertEquals(1, new ReadingsKey(d).compareTo(inner));
            Assert.assertEquals(0, new ReadingsKey(new GregorianCalendar(2026, Calendar.JANUARY, 1).getTime())
                    .compareTo(book.getKey(mediumName(date(2026, Calendar.JANUARY, 1))).get(0)));
        }
    }

    @Test
    public void testValidKeyAndEmptyKeyList() {
        Key valid = book.getValidKey(mediumName(date(2026, Calendar.MARCH, 3)));
        Assert.assertEquals(1, valid.getCardinality());
        Key empty = book.createEmptyKeyList();
        Assert.assertEquals(0, empty.getCardinality());
        Assert.assertNotSame(empty, book.createEmptyKeyList());
    }

    @Test
    public void testPreferredKeyIsTodayAndInTheGlobalList() {
        Assert.assertTrue(book instanceof PreferredKey);
        Key preferred = ((PreferredKey) book).getPreferred();
        Assert.assertTrue(preferred instanceof ReadingsKey);
        Assert.assertEquals(mediumName(new Date()), preferred.getName());
        Assert.assertTrue(book.getGlobalKeyList().contains(preferred));
    }

    @Test
    public void testOsisOfTheFirstDay() throws Exception {
        Key key = book.getGlobalKeyList().get(0);
        Iterator<Content> it = book.getOsisIterator(key, false, false);
        Assert.assertTrue(it.hasNext());
        Element title = (Element) it.next();
        Assert.assertEquals("title", title.getName());
        Assert.assertEquals(key.getName(), title.getText());
        Element list = (Element) it.next();
        Assert.assertFalse(it.hasNext());
        Assert.assertEquals("list", list.getName());
        // 01.01=Gen.1-Gen.2 Ps.1-Ps.2 Matt.1-Matt.2
        List<Element> items = list.getChildren("item", list.getNamespace());
        Assert.assertEquals(3, items.size());
        Element reference = items.get(0).getChildren().get(0);
        Assert.assertEquals("reference", reference.getName());
        Assert.assertEquals("Gen.1-Gen.2", reference.getAttributeValue("osisRef"));
        Assert.assertEquals("Genesis 1-2", reference.getText());
        Assert.assertEquals("Ps.1-Ps.2", items.get(1).getChildren().get(0).getAttributeValue("osisRef"));
        Assert.assertEquals("Matt.1-Matt.2", items.get(2).getChildren().get(0).getAttributeValue("osisRef"));
    }

    @Test
    public void testOsisIteratorRejectsOtherKeys() throws Exception {
        try {
            book.getOsisIterator(new DefaultLeafKeyList("x"), false, false);
            Assert.fail();
        } catch (BookException expected) {
            Assert.assertEquals("Key not found x", expected.getMessage());
        }
        // A readings key of a year that is not in the set
        Key other = new ReadingsKey(date(1999, Calendar.JANUARY, 1));
        try {
            book.getOsisIterator(other, false, false);
            Assert.fail();
        } catch (BookException expected) {
            Assert.assertEquals("Key not found " + other.getName(), expected.getMessage());
        }
    }

    @Test
    public void testReadOnlyBehaviour() throws Exception {
        Key key = book.getGlobalKeyList().get(0);
        Assert.assertEquals("", book.getRawText(key));
        Assert.assertFalse(book.contains(key));
        Assert.assertFalse(book.contains(null));
        try {
            book.setRawText(key, "x");
            Assert.fail();
        } catch (BookException expected) {
            Assert.assertEquals("This Book is read-only.", expected.getMessage());
        }
        try {
            book.setAliasKey(key, key);
            Assert.fail();
        } catch (BookException expected) {
            Assert.assertEquals("This Book is read-only.", expected.getMessage());
        }
    }

    @Test
    public void testKeyEqualsHashCodeAndCompare() {
        Date d1 = date(2026, Calendar.JANUARY, 1);
        ReadingsKey a = new ReadingsKey(d1);
        ReadingsKey same = new ReadingsKey(new Date(d1.getTime()));
        ReadingsKey later = new ReadingsKey(date(2026, Calendar.FEBRUARY, 2));

        Assert.assertEquals(a, a);
        Assert.assertEquals(a, same);
        Assert.assertEquals(a.hashCode(), same.hashCode());
        Assert.assertEquals(d1.hashCode(), a.hashCode());
        Assert.assertEquals(0, a.compareTo(same));
        Assert.assertNotEquals(a, later);
        Assert.assertTrue(a.compareTo(later) < 0);
        Assert.assertTrue(later.compareTo(a) > 0);
        Assert.assertNotEquals(a, null);
        // Equality is by name and exact class
        Assert.assertNotEquals(a, new DefaultLeafKeyList(a.getName()));

        // The osis name has the form d.MMMM
        Assert.assertEquals(new SimpleDateFormat("d.MMMM").format(d1), a.getOsisRef());
        Assert.assertEquals(a.getName(), a.getName(null));
    }

    @Test(expected = ClassCastException.class)
    public void testCompareToOtherKeyTypeFails() {
        new ReadingsKey(date(2026, Calendar.JANUARY, 1)).compareTo(new DefaultLeafKeyList("x"));
    }

    @Test
    public void testKeyClone() {
        ReadingsKey a = new ReadingsKey(date(2026, Calendar.JANUARY, 1));
        ReadingsKey copy = a.clone();
        Assert.assertNotSame(a, copy);
        Assert.assertEquals(a, copy);
        Assert.assertEquals(0, a.compareTo(copy));
        Assert.assertEquals(a.hashCode(), copy.hashCode());
    }
}
