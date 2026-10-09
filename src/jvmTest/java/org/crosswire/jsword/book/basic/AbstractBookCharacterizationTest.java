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
 */
package org.crosswire.jsword.book.basic;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.BookMetaData;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.index.IndexStatus;
import org.crosswire.jsword.index.IndexStatusEvent;
import org.crosswire.jsword.index.IndexStatusListener;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.PassageKeyFactory;
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor;
import org.crosswire.jsword.versification.system.Versifications;
import org.jdom2.Content;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Characterization of {@link AbstractBook} (through the installed KJV and
 * StrongsGreek, and through a minimal subclass built in the test) and of
 * {@link DefaultBookMetaData}.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class AbstractBookCharacterizationTest {
    private static Book installed(String initials) {
        Book book = Books.installed().getBook(initials);
        Assume.assumeNotNull(book);
        return book;
    }

    /** Minimal concrete AbstractBook: delegation is what is under test. */
    private static class StubBook extends AbstractBook {
        private final Key globalKeys;
        private final boolean osisFails;
        final AtomicInteger osisCalls = new AtomicInteger();

        StubBook(BookMetaData bmd, Key globalKeys, boolean osisFails) {
            super(bmd, null);
            this.globalKeys = globalKeys;
            this.osisFails = osisFails;
        }

        @Override
        protected List<Content> getOsis(Key key, RawTextToXmlProcessor p) throws BookException {
            osisCalls.incrementAndGet();
            if (osisFails) {
                throw new BookException("cannot read");
            }
            return null;
        }

        public Key getGlobalKeyList() {
            return globalKeys;
        }

        public Key getValidKey(String name) {
            return null;
        }

        public Key getKey(String name) {
            return null;
        }

        public Key createEmptyKeyList() {
            return null;
        }

        public java.util.Iterator<Content> getOsisIterator(Key key, boolean allowEmpty, boolean allowGenTitles) {
            return null;
        }

        public boolean contains(Key key) {
            return false;
        }

        public String getRawText(Key key) {
            return null;
        }

        public boolean isWritable() {
            return false;
        }

        public void setRawText(Key key, String rawData) {
        }

        public void setAliasKey(Key alias, Key source) {
        }
    }

    /** A metadata whose unlock() succeeds and which records resetLock(). */
    private static class UnlockableMetaData extends DefaultBookMetaData {
        final AtomicInteger resets = new AtomicInteger();

        UnlockableMetaData() {
            super(null, "Test Book", BookCategory.BIBLE);
        }

        @Override
        public boolean unlock(String unlockKey) {
            return true;
        }

        @Override
        public void resetLock() {
            resets.incrementAndGet();
        }
    }

    private static Key emptyKeyList() {
        return PassageKeyFactory.instance().createEmptyKeyList(Versifications.instance().getVersification("KJV"));
    }

    // ---- installed books ----

    @Test
    public void testEqualsHashCodeCompareToOnInstalledBooks() {
        Book kjv = installed("KJV");
        Book strongs = installed("StrongsGreek");
        Book esv = installed("ESV2011");

        Assert.assertTrue(kjv.equals(kjv));
        Assert.assertEquals(0, kjv.compareTo(kjv));
        Assert.assertEquals(kjv.getBookMetaData().hashCode(), kjv.hashCode());
        Assert.assertFalse(kjv.equals(null));

        // Different concrete classes (SwordBook vs SwordDictionary) are never equal.
        Assert.assertFalse(kjv.equals(strongs));
        Assert.assertFalse(strongs.equals(kjv));
        // Same class, different metadata (initials/name differ) are not equal.
        Assert.assertFalse(kjv.equals(esv));

        // compareTo is the metadata compareTo: Bible category sorts before Dictionary,
        // then abbreviation ignoring case ("ESV2011" < "KJV").
        Assert.assertTrue(kjv.compareTo(strongs) < 0);
        Assert.assertTrue(strongs.compareTo(kjv) > 0);
        Assert.assertTrue(esv.compareTo(kjv) < 0);
        Assert.assertTrue(kjv.compareTo(esv) > 0);
    }

    @Test
    public void testToStringAndMetadataDelegation() {
        Book kjv = installed("KJV");
        BookMetaData meta = kjv.getBookMetaData();
        Assert.assertEquals(meta.toString(), kjv.toString());
        Assert.assertTrue(kjv.toString().startsWith("KJV"));
        Assert.assertSame(meta, kjv.getBookMetaData());
        Assert.assertEquals(meta.getInitials(), kjv.getInitials());
        Assert.assertEquals(meta.getName(), kjv.getName());
        Assert.assertEquals(meta.getAbbreviation(), kjv.getAbbreviation());
        Assert.assertEquals(meta.getOsisID(), kjv.getOsisID());
        Assert.assertEquals(meta.getLanguage(), kjv.getLanguage());
        Assert.assertEquals(meta.getBookCategory(), kjv.getBookCategory());
        Assert.assertEquals(meta.getDriver(), kjv.getDriver());
        Assert.assertEquals(meta.getDriverName(), kjv.getDriverName());
        Assert.assertEquals(meta.getPropertyKeys(), kjv.getPropertyKeys());
    }

    @Test
    public void testPropertyAndFlagsOnInstalledBooks() {
        for (String initials : new String[] {"KJV", "StrongsGreek"}) {
            Book book = installed(initials);
            Assert.assertEquals(initials, "en", book.getProperty("Lang"));
            Assert.assertFalse(initials, book.isEnciphered());
            Assert.assertFalse(initials, book.isLocked());
            Assert.assertNull(initials, book.getUnlockKey());
            Assert.assertTrue(initials, book.isSupported());
            Assert.assertNull(initials, book.getProperty("NoSuchPropertyKey"));
            Assert.assertSame(initials, book, ((AbstractBook) book).getBook());
            Assert.assertTrue(initials, book.isLeftToRight());
        }
        Assert.assertEquals(BookCategory.BIBLE, installed("KJV").getBookCategory());
        Assert.assertEquals(BookCategory.DICTIONARY, installed("StrongsGreek").getBookCategory());
    }

    @Test
    public void testMatch() {
        Book kjv = installed("KJV");
        Assert.assertFalse(kjv.match(null));
        Assert.assertTrue(kjv.match("KJV"));
        Assert.assertTrue(kjv.match("kjv"));
        Assert.assertTrue(kjv.match(kjv.getName()));
        Assert.assertTrue(kjv.match(kjv.getName().toUpperCase()));
        // startsWith the initials
        Assert.assertTrue(kjv.match("KJV extra"));
        Assert.assertFalse(kjv.match("XYZ"));
        Assert.assertFalse(kjv.match(""));
    }

    @Test
    public void testGetValidKeyOnPassageBook() {
        Book kjv = installed("KJV");
        Key key = kjv.getValidKey("Gen 1:1");
        Assert.assertEquals("Genesis 1:1", key.getName());
        Assert.assertEquals(1, key.getCardinality());
        Assert.assertTrue(kjv.contains(key));

        // Unparsable text gives an empty key list, not an exception.
        Key bad = kjv.getValidKey("nonsense");
        Assert.assertNotNull(bad);
        Assert.assertEquals(0, bad.getCardinality());
        Assert.assertEquals("", bad.getName());
    }

    @Test
    public void testScopeIsGlobalKeyList() {
        Book kjv = installed("KJV");
        Assert.assertSame(kjv.getGlobalKeyList(), kjv.getScope());
    }

    // ---- DefaultBookMetaData ----

    @Test
    public void testDefaultBookMetaData() {
        DefaultBookMetaData meta = new DefaultBookMetaData(null, "Test Book", BookCategory.COMMENTARY);
        Assert.assertEquals("Test Book", meta.getName());
        // initials are derived from the words of the name; abbreviation is the same
        Assert.assertEquals("TB", meta.getInitials());
        Assert.assertEquals("TB", meta.getAbbreviation());
        Assert.assertEquals("TB", meta.toString());
        Assert.assertEquals(BookCategory.COMMENTARY, meta.getBookCategory());
        Assert.assertEquals("Test Book".hashCode(), meta.hashCode());

        // setName and setBookCategory write properties
        Assert.assertEquals("Test Book", meta.getProperty(BookMetaData.KEY_NAME));
        Assert.assertEquals(BookCategory.COMMENTARY.toString(), meta.getProperty(BookMetaData.KEY_CATEGORY));
        Assert.assertEquals(2, meta.getPropertyKeys().size());
        Assert.assertTrue(meta.getPropertyKeys().contains(BookMetaData.KEY_NAME));
        Assert.assertTrue(meta.getPropertyKeys().contains(BookMetaData.KEY_CATEGORY));

        // missing key is null; the language key is answered from the language object
        Assert.assertNull(meta.getProperty("missing"));
        Assert.assertNull(meta.getValues("missing"));
        Assert.assertEquals(meta.getLanguage().getName(), meta.getProperty(BookMetaData.KEY_LANGUAGE));

        meta.putProperty("custom", "v");
        Assert.assertEquals("v", meta.getProperty("custom"));
        Assert.assertEquals(java.util.Collections.singletonList("v"), new java.util.ArrayList<String>(meta.getValues("custom")));
        meta.putProperty("custom2", "w", true);
        Assert.assertEquals("w", meta.getProperty("custom2"));
        Assert.assertEquals(4, meta.getPropertyKeys().size());

        // the key set is a read-only view
        try {
            meta.getPropertyKeys().add("x");
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void testDefaultBookMetaDataDefaultsAndEquality() {
        DefaultBookMetaData a = new DefaultBookMetaData(null, "Test Book", null);
        // null category falls back to BIBLE
        Assert.assertEquals(BookCategory.BIBLE, a.getBookCategory());
        Assert.assertEquals(BookCategory.BIBLE.getName() + ".TB", a.getOsisID());
        Assert.assertTrue(a.isSupported());
        Assert.assertFalse(a.isEnciphered());
        Assert.assertFalse(a.isLocked());
        Assert.assertFalse(a.unlock("x"));
        Assert.assertNull(a.getUnlockKey());
        Assert.assertEquals(IndexStatus.UNDONE, a.getIndexStatus());
        Assert.assertEquals("UTF-8", a.getBookCharset());

        DefaultBookMetaData b = new DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE);
        DefaultBookMetaData c = new DefaultBookMetaData(null, "Other Book", BookCategory.BIBLE);
        Assert.assertEquals(a, b);
        Assert.assertNotEquals(a, c);
        Assert.assertNotEquals(a, null);
        Assert.assertEquals(0, a.compareTo(b));

        a.setInitials(null);
        Assert.assertEquals("TB", a.getInitials());
    }

    // ---- AbstractBook behaviour through a stub ----

    @Test
    public void testStubDelegationAndPropertyPut() {
        DefaultBookMetaData meta = new DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE);
        StubBook book = new StubBook(meta, null, false);
        Assert.assertSame(meta, book.getBookMetaData());
        Assert.assertNull(book.getDriver());
        Assert.assertNull(book.getDriverName());
        Assert.assertNull(book.getBackend());
        Assert.assertSame(book, book.getBook());
        Assert.assertEquals("TB", book.getInitials());
        Assert.assertEquals("TB", book.toString());

        book.putProperty("k", "v");
        Assert.assertEquals("v", meta.getProperty("k"));
        book.putProperty("k2", "v2", true);
        Assert.assertEquals("v2", book.getProperty("k2"));

        BookMetaData other = new DefaultBookMetaData(null, "Other Book", BookCategory.BIBLE);
        book.setBookMetaData(other);
        Assert.assertSame(other, book.getBookMetaData());
        Assert.assertEquals("OB", book.getInitials());

        // equals requires the same runtime class and equal metadata
        StubBook twin = new StubBook(new DefaultBookMetaData(null, "Other Book", BookCategory.BIBLE), null, false);
        Assert.assertEquals(book, twin);
        Assert.assertEquals(book.hashCode(), twin.hashCode());
        Assert.assertEquals(0, book.compareTo(twin));
    }

    @Test
    public void testIndexStatusFiresOnlyOnChange() {
        DefaultBookMetaData meta = new DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE);
        StubBook book = new StubBook(meta, null, false);
        final AtomicInteger events = new AtomicInteger();
        final IndexStatus[] last = new IndexStatus[1];
        IndexStatusListener listener = new IndexStatusListener() {
            public void statusChanged(IndexStatusEvent ev) {
                events.incrementAndGet();
                last[0] = ev.getIndexStatus();
            }
        };
        book.addIndexStatusListener(listener);

        Assert.assertEquals(IndexStatus.UNDONE, book.getIndexStatus());
        book.setIndexStatus(IndexStatus.UNDONE);
        Assert.assertEquals(0, events.get());
        book.setIndexStatus(IndexStatus.DONE);
        Assert.assertEquals(1, events.get());
        Assert.assertEquals(IndexStatus.DONE, last[0]);
        Assert.assertEquals(IndexStatus.DONE, meta.getIndexStatus());

        book.removeIndexStatusListener(listener);
        book.setIndexStatus(IndexStatus.UNDONE);
        Assert.assertEquals(1, events.get());
        Assert.assertEquals(IndexStatus.UNDONE, book.getIndexStatus());
    }

    @Test
    public void testUnlockWhenMetadataRefuses() {
        StubBook book = new StubBook(new DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE), null, false);
        Assert.assertFalse(book.unlock("x"));
        Assert.assertEquals(0, book.osisCalls.get());
    }

    @Test
    public void testUnlockWithNullGlobalKeyListIsAssumedValid() {
        UnlockableMetaData meta = new UnlockableMetaData();
        StubBook book = new StubBook(meta, null, false);
        Assert.assertTrue(book.unlock("x"));
        Assert.assertEquals(0, meta.resets.get());
        Assert.assertEquals(0, book.osisCalls.get());
    }

    @Test
    public void testUnlockTriesToReadAndResetsOnFailure() {
        UnlockableMetaData meta = new UnlockableMetaData();
        StubBook failing = new StubBook(meta, emptyKeyList(), true);
        Assert.assertFalse(failing.unlock("x"));
        Assert.assertEquals(1, failing.osisCalls.get());
        Assert.assertEquals(1, meta.resets.get());

        UnlockableMetaData meta2 = new UnlockableMetaData();
        StubBook working = new StubBook(meta2, emptyKeyList(), false);
        Assert.assertTrue(working.unlock("x"));
        Assert.assertEquals(1, working.osisCalls.get());
        Assert.assertEquals(0, meta2.resets.get());
    }
}
