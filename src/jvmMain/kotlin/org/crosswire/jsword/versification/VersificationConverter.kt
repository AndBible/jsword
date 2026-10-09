package org.crosswire.jsword.versification

import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.Passage
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.RangedPassage
import org.crosswire.jsword.passage.RestrictionType
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Manage conversion of verses to a specific versification
 *
 * @author Martin Denham [mjdenham at gmail dot com]
 */
open class VersificationConverter {
    private val versificationsMapper: VersificationsMapper = VersificationsMapper.instance()

    /**
     * Flexible converter for the generic Key base class.
     * Return the key in the required versification, mapping if necessary
     * Currently only handles Passage, RangedPassage, and Verse
     */
    open fun convert(key: Key?, toVersification: Versification?): Key {
        try {
            if (key is RangedPassage) {
                return convert(key, toVersification)
            } else if (key is VerseRange) {
                return convert(key, toVersification)
            } else if (key is Passage) {
                return convert(key, toVersification)
            } else if (key is Verse) {
                return convert(key, toVersification)
            }
        } catch (e: Exception) {
            // unexpected problem during mapping
            log.warn("JSword Versification mapper failed to map " + key!!.getOsisID() + " to " + toVersification!!.name)
        }
        return PassageKeyFactory.instance().createEmptyKeyList(toVersification)
    }

    open fun convert(passage: Passage?, toVersification: Versification?): Passage {
        return versificationsMapper.map(passage, toVersification)
    }

    open fun convert(rangedPassage: RangedPassage?, toVersification: Versification?): RangedPassage {
        val result = RangedPassage(toVersification)
        val iter = rangedPassage!!.rangeIterator(RestrictionType.NONE)
        while (iter.hasNext()) {
            result.add(convert(iter.next(), toVersification))
        }
        return result
    }

    open fun convert(verseRange: VerseRange?, toVersification: Versification?): VerseRange {
        val startVerse = verseRange!!.start
        val endVerse = verseRange.end

        val convertedStartVerse = convert(startVerse, toVersification)
        val convertedEndVerse = convert(endVerse, toVersification)

        return VerseRange(toVersification, convertedStartVerse, convertedEndVerse)
    }

    /** Return the verse in the required versification, mapping if necessary
     */
    open fun convert(verse: Verse?, toVersification: Versification?): Verse {
        val mappedVerse = convertVerseStrictly(verse, toVersification)
        if (mappedVerse != null) return mappedVerse
        // just try to retain information by forcing creation of a similar verse with the new v11n
        return Verse(toVersification, verse!!.book, verse.chapter, verse.verse)
    }

    open fun isConvertibleTo(verse: Verse?, v11n: Versification?): Boolean {
        return convertVerseStrictly(verse, v11n) != null
    }

    /**
     * Convert the verse correctly to the v11n or return null
     */
    private fun convertVerseStrictly(verse: Verse?, toVersification: Versification?): Verse? {
        try {
            val key = versificationsMapper.mapVerse(verse, toVersification)
            if (!key.isEmpty()) {
                val mappedVerse = KeyUtil.getVerse(key)
                // If target v11n does not contain mapped verse then an exception normally occurs and the ordinal is set to 0
                if (mappedVerse!!.ordinal > 0) {
                    return mappedVerse
                }
            }
        } catch (e: Exception) {
            // unexpected problem during mapping
            log.warn("JSword Versification mapper failed to map " + verse!!.getOsisID() + " from " + verse.versification.name + " to " + toVersification!!.name)
            throw e
        }
        return null
    }

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(VersificationConverter::class.java)
    }
}
