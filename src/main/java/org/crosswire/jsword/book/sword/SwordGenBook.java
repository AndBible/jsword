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
package org.crosswire.jsword.book.sword;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.crosswire.common.activate.Activator;
import org.crosswire.common.activate.Lock;
import org.crosswire.jsword.JSMsg;
import org.crosswire.jsword.JSOtherMsg;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.basic.AbstractBook;
import org.crosswire.jsword.book.filter.SourceFilter;
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor;
import org.crosswire.jsword.passage.DefaultKeyList;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.crosswire.jsword.passage.ReadOnlyKeyList;
import org.crosswire.jsword.passage.VerseRange;
import org.jdom2.Content;

/**
 * A Sword version of a generic book.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
public class SwordGenBook extends AbstractBook {
    /**
     * Construct an SwordGenBook given the BookMetaData and the AbstractBackend.
     * 
     * @param sbmd the metadata that describes the book
     * @param backend the means by which the resource is accessed
     */
    public SwordGenBook(SwordBookMetaData sbmd, Backend backend) {
        super(sbmd, backend);

        if (backend == null) {
            throw new IllegalArgumentException("AbstractBackend must not be null.");
        }

        this.filter = sbmd.getFilter();
        map = null;
        set = null;
        global = null;
        active = false;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#activate(org.crosswire.common.activate.Lock)
     */
    @Override
    public final synchronized void activate(Lock lock) {
        super.activate(lock);

        Key newSet = getBackend().readIndex();
        Map<String, Key> newMap = new HashMap<String, Key>();
        for (Key key : newSet) {
            newMap.put(key.getOsisRef(), key);
        }

        // Publish fully-populated state before flipping active=true so that
        // a concurrent reader observing active=true is guaranteed to see a
        // non-null, fully-populated map.
        set = newSet;
        map = newMap;
        global = new ReadOnlyKeyList(newSet, false);
        active = true;

        // We don't need to activate the backend because it should be capable
        // of doing it for itself.
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#deactivate(org.crosswire.common.activate.Lock)
     */
    @Override
    public final synchronized void deactivate(Lock lock) {
        super.deactivate(lock);

        // Flip active=false BEFORE nulling the fields so that a concurrent
        // reader passing checkActive() (active==true) is guaranteed to still
        // see non-null map/set/global. Otherwise a race between deactivate
        // and getKey() can produce NPE on map.get().
        active = false;

        map = null;
        set = null;
        global = null;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getOsisIterator(org.crosswire.jsword.passage.Key, boolean, boolean)
     */
    public Iterator<Content> getOsisIterator(Key key, final boolean allowEmpty, final boolean allowGenTitle) throws BookException {
        checkActive();

        assert key != null;

        return getBackend().readToOsis(key, new RawTextToXmlProcessor() {
            public void preRange(VerseRange range, List<Content> partialDom) {
                // no - op
            }

            public void postVerse(Key verse, List<Content> partialDom, String rawText) {
                partialDom.addAll(filter.toOSIS(SwordGenBook.this, verse, rawText));
            }

            public void init(List<Content> partialDom) {
                // no-op
            }
        }).iterator();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getRawText(org.crosswire.jsword.passage.Key)
     */
    public String getRawText(Key key) throws BookException {
        return getBackend().getRawText(key);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#contains(org.crosswire.jsword.passage.Key)
     */
    public boolean contains(Key key) {
        return getBackend().contains(key);
    }

    /*
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#getOsis(org.crosswire.jsword.passage.Key, org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor)
     */
    @Override
    public List<Content> getOsis(Key key, RawTextToXmlProcessor processor) throws BookException {
        checkActive();

        assert key != null;

        return getBackend().readToOsis(key, processor);
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isWritable()
     */
    public boolean isWritable() {
        return getBackend().isWritable();
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    public void setRawText(Key key, String rawData) throws BookException {
        throw new BookException(JSOtherMsg.lookupText("This Book is read-only."));
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    public void setAliasKey(Key alias, Key source) throws BookException {
        throw new BookException(JSOtherMsg.lookupText("This Book is read-only."));
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#getGlobalKeyList()
     */
    public Key getGlobalKeyList() {
        checkActive();

        // Snapshot the field into a local reference. A concurrent deactivate()
        // may null the global field after checkActive() has returned, so
        // returning the field directly could hand back null to the caller. If
        // we lost that race, force a re-activation and re-snapshot before
        // falling back to an empty key list.
        Key localGlobal = global;
        if (localGlobal == null) {
            Activator.activate(this);
            localGlobal = global;
        }
        if (localGlobal == null) {
            return createEmptyKeyList();
        }

        return localGlobal;
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#getValidKey(java.lang.String)
     */
    public Key getValidKey(String name) {
        try {
            return getKey(name);
        } catch (NoSuchKeyException e) {
            return createEmptyKeyList();
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#getKey(java.lang.String)
     */
    public Key getKey(String text) throws NoSuchKeyException {
        checkActive();

        // Snapshot the map into a local reference. A concurrent deactivate()
        // may null the map field after checkActive() has returned, so working
        // off the field directly could hit map.get() on null. If we lost that
        // race, force a re-activation and re-snapshot before giving up.
        Map<String, Key> localMap = map;
        if (localMap == null) {
            Activator.activate(this);
            localMap = map;
        }
        if (localMap == null) {
            throw new NoSuchKeyException(JSMsg.gettext("No entry for '{0}' in {1}.", text, getInitials()));
        }

        Key key = localMap.get(text);
        if (key != null) {
            return key;
        }

        // First check for keys that match ignoring case
        for (String keyName : localMap.keySet()) {
            if (keyName.equalsIgnoreCase(text)) {
                return localMap.get(keyName);
            }
        }

        // Next keys that start with the given text
        for (String keyName : localMap.keySet()) {
            if (keyName.startsWith(text)) {
                return localMap.get(keyName);
            }
        }

        // Next try keys that contain the given text
        for (String keyName : localMap.keySet()) {
            if (keyName.indexOf(text) != -1) {
                return localMap.get(keyName);
            }
        }

        // TRANSLATOR: Error condition: Indicates that something could not be
        // found in the book.
        // {0} is a placeholder for the unknown key.
        // {1} is the short name of the book
        throw new NoSuchKeyException(JSMsg.gettext("No entry for '{0}' in {1}.", text, getInitials()));
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#createEmptyKeyList()
     */
    public Key createEmptyKeyList() {
        return new DefaultKeyList();
    }

    /**
     * Helper method so we can quickly activate ourselves on access
     */
    private void checkActive() {
        if (!active) {
            Activator.activate(this);
        }
    }

    /**
     * The global key list
     */
    private Key global;

    /**
     * Are we active.
     * Volatile so that a reader thread sees the latest value without
     * needing to enter the synchronized block (cf. checkActive()).
     */
    private volatile boolean active;

    /**
     * So we can quickly find a Key given the text for the key
     */
    private Map<String, Key> map;

    /**
     * So we can implement getIndex() easily
     */
    private Key set;

    /**
     * The filter to use to convert to OSIS.
     */
    protected SourceFilter filter;

}
