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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
package org.crosswire.jsword.versification

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.MissingResourceException

import org.crosswire.common.config.ConfigException
import org.crosswire.jsword.passage.Passage
import org.crosswire.jsword.passage.RangedPassage
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseKey
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * VersificationMapper maps a Verse or a Passage in one Versification (v11n)
 * to another, using the KJV v11n as an intermediary.
 * <p>
 * Practically speaking, a Verse in one v11n may map:
 * <ul>
 * <li>to the same verse in another v11n. This is the typical case.</li>
 * <li>to another verse in the same chapter. This is common and typically by one, shifting following verses.</li>
 * <li>to another verse in a different chapter. This is fairly common.</li>
 * <li>to two other verses. This is common in the Psalms and a few places elsewhere.</li>
 * </ul>
 * <p>
 * The internal details of the mapping can be found in VersificationToKJVMapper.
 * </p>
 * <p>
 * This transitive relationship is not perfect. It assumes that verses
 * outside of the KJV versification map 1:1 between the source and
 * target Versifications. That it uses the KJV as an intermediary is an
 * implementation detail that may change. Do not rely on it.
 * </p>
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Chris Burrell
 */
class VersificationsMapper
/**
 * Prevent instantiation
 */
private constructor() {
    init {
        // we have no mapper for the KJV, since everything maps map to the KJV, so we'll simply add an entry
        // in there to avoid ever trying to load it
        MAPPERS[KJV] = null
    }

    /**
     * Maps a whole passage, and does so verse by verse. We can't do any better, since, we may for
     * example have:
     * Ps.1.1-Ps.1.10 =&gt; Ps.1.2-Ps.1.11 so one would think we can simply map each of the start and end verses.
     * However, this would be inaccurate since verse 9 might map to verse 12, 13, etc.
     *
     * @param key    the key if the source versification
     * @param target the target versification
     * @return the new key in the new versification
     */
    fun map(key: Passage?, target: Versification?): Passage {
        if (key!!.versification == target) {
            return key
        }

        val newPassage: Passage = RangedPassage(target)
        val verses = key.iterator()
        while (verses.hasNext()) {
            val verse = verses.next() as Verse
            newPassage.addAll(this.mapVerse(verse, target))
        }

        return newPassage
    }

    /**
     * @param v                   the verse
     * @param targetVersification the final versification that we want
     * @return the key for the verse
     */
    fun mapVerse(v: Verse?, targetVersification: Versification?): VerseKey<*> {
        if (v!!.versification == targetVersification) {
            return v
        }

        ensure(v.versification)
        ensure(targetVersification)

        // caution, mappers can be null if they are missing or failed to load.
        // get the source mapper, to get to the KJV
        val mapper = MAPPERS[v.versification]

        // mapped verses could be more than 1 verse in KJV
        val kjvVerses: MutableList<QualifiedKey>
        if (mapper == null) {
            // we can't map to the KJV, so we're going to take a wild guess
            // and return the equivalent verse
            // and assume that it maps directly on to the KJV,
            // and thereby continue with the process
            kjvVerses = ArrayList()
            val reversifiedVerse = v.reversify(KJV)
            //check that the key actually exists
            if (reversifiedVerse != null) {
                kjvVerses.add(QualifiedKey(reversifiedVerse))
            }
        } else {
            //we need qualified keys back, so as to preserve parts
            kjvVerses = mapper.map(QualifiedKey(v))
        }

        if (KJV == targetVersification) {
            // we're done, so simply return the key we have so far.
            return getKeyFromQualifiedKeys(KJV, kjvVerses)
        }

        // we're continuing, so we need to unmap from the KJV qualified key onto
        // the new versification.
        val targetMapper = MAPPERS[targetVersification]
        if (targetMapper == null) {
            // failed to load, so we'll do our wild-guess again,
            // and assume that the KJV keys map to the target
            return guessKeyFromKjvVerses(targetVersification, kjvVerses)
        }

        // we can use the unmap method for that. Since we have a list of
        // qualified keys, we do so for every qualified
        // key in the list - this means that parts would get transported as
        // well.
        val finalKeys: VerseKey<*> = RangedPassage(targetVersification)
        for (qualifiedKey in kjvVerses) {
            val verseKey = targetMapper.unmap(qualifiedKey)
            if (verseKey != null) {
                //verse key exists in the target versification
                finalKeys.addAll(verseKey)
            }
        }
        return finalKeys
    }

    /**
     * This is a last attempt at trying to get something, on the basis that
     * something is better than nothing.
     *
     * @param targetVersification the target versification
     * @param kjvVerses           the verses in the KJV versification.
     * @return the possible verses in the target versification, no guarantees
     * made
     */
    private fun guessKeyFromKjvVerses(targetVersification: Versification?, kjvVerses: List<QualifiedKey>): VerseKey<*> {
        val finalKeys: VerseKey<*> = RangedPassage(targetVersification)
        for (qualifiedKey in kjvVerses) {
            if (qualifiedKey.key != null) {
                val key = qualifiedKey.reversify(targetVersification).key
                if (key != null) {
                    //verse key exists in target versification
                    finalKeys.addAll(key)
                }
            }
        }
        return finalKeys
    }

    /**
     * @param kjvVerses the list of keys
     * @return the aggregate key
     */
    private fun getKeyFromQualifiedKeys(versification: Versification?, kjvVerses: List<QualifiedKey>): VerseKey<*> {
        val finalKey: VerseKey<*> = RangedPassage(versification)
        for (k in kjvVerses) {
            // we simply ignore everything else at this stage. The other bits
            // and pieces are used while we're converting
            // from one to the other.
            if (k.key != null) {
                finalKey.addAll(k.key)
            }
        }
        return finalKey
    }

    /** 
     * Call this to ensure mapping data is loaded (maybe for newly installed books).  
     * Should normally be called from a background thread, not the ui thread.

     * @param versification the versification we want to load mapping data for
     */
    fun ensureMappingDataLoaded(versification: Versification?) {
        ensure(versification)
    }

    /**
     * Reads the mapping from file if it does not exist
     *
     * @param versification the versification we want to load
     */
    private fun ensure(versification: Versification?) {
        if (MAPPERS.containsKey(versification)) {
            return
        }

        try {
            MAPPERS[versification] = VersificationToKJVMapper(versification, FileVersificationMapping(versification))
        } catch (e: IOException) {
            // we've attempted to load it once, and that's all we'll do.
            LOGGER.error("Failed to load versification mappings for versification [{}]", versification, e)
            MAPPERS[versification] = null
        } catch (e: ConfigException) {
            // we've attempted to load it once, and that's all we'll do.
            LOGGER.error("Failed to load versification mappings for versification [{}]", versification, e)
            MAPPERS[versification] = null
        } catch (e: MissingResourceException) {
            // we've attempted to load it once, and that's all we'll do.
            LOGGER.error("Failed to load versification mappings for versification [{}]", versification, e)
            MAPPERS[versification] = null
        }
    }

    companion object {
        @Volatile
        private var instance: VersificationsMapper? = null
        private val KJV: Versification = Versifications.instance().getVersification(SystemKJVA.V11N_NAME)!!
        private val MAPPERS: MutableMap<Versification?, VersificationToKJVMapper?> = HashMap()
        private val LOGGER: Logger = LoggerFactory.getLogger(VersificationsMapper::class)

        /**
         * @return a singleton instance of the mapper -
         */
        @JvmStatic
        fun instance(): VersificationsMapper {
            if (instance == null) {
                synchronized(VersificationsMapper::class.java) {
                    if (instance == null) {
                        instance = VersificationsMapper()
                    }
                }
            }
            return instance!!
        }
    }
}
