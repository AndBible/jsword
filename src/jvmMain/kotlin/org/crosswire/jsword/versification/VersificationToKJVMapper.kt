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


import org.crosswire.common.util.KeyValuePair
import org.crosswire.common.util.LucidRuntimeException
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.NoSuchVerseException
import org.crosswire.jsword.passage.OsisParser
import org.crosswire.jsword.passage.Passage
import org.crosswire.jsword.passage.RangedPassage
import org.crosswire.jsword.passage.RestrictionType
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseKey
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * A Versification mapper allows you to a map a given verse to the KJV versification,
 * or unmap it from the KJV versification into your own versification.
 * Note : in order to allow for all known bible contents (including deuterocanonical texts) to be mapped, within this
 * class "KJV" actually stands for the "KJVA" versification system.
 *
 * <p>
 * A properties-like file will contain the non-KJV versification as they key, and the KJV versification value
 * as the target value... Duplicate keys are allowed.
 * </p>
 * <p>
 * i.e. Gen.1.1=Gen.1.2 means Gen.1.1 in X versification is Gen.1.2 in the KJV versification
 * </p>
 * <p>
 * You can specify a range on either side. If a range is present on both sides, they have to have the same number of
 * verses, i.e. verses are mapped verse by verse to each other<br>
 * Gen.1.1-Gen.1.2=Gen.1.2-Gen.1.3 means Gen.1.1=Gen.1.2 and Gen.1.2=Gen.1.3<br>
 *<br>
 * Note: if the cardinality of the left &amp; KJV sides are different by only one, the algorithm makes the
 * assumption that verse 0 should be disregarded in both ranges.
 * </p>
 * <p>
 * Mappings can be specified by offset. In this case, be aware this maps verse 0 as well. So for example:
 * </p>
 * <p>
 * Ps.19-Ps.20=-1 means Ps.19.0=Ps.18.50, Ps.19.1=Ps.19.0, Ps.19.2=Ps.19.1, etc.<br>
 * It does not make much sense to have an offset for a single verse, so this is not supported.
 * Offsetting for multiple ranges however does, and operates range by range, i.e. each range is calculated separately.
 * Offsetting is somewhat equivalent to specifying ranges, and as a result, the verse 0 behaviour is identical.
 * </p>
 * <p>
 * You can use part-mappings. This is important if you want to preserve transformations from one side to another without
 * losing resolution of the verse.
 * </p>
 * <p>
 * For example,<br>
 * if V1 defines Gen.1.1=Gen.1.1, Gen.1.2=Gen1.1<br>
 * if V2 defines Gen.1.1=Gen.1.1, Gen.1.2=Gen.1.1<br>
 * then, mapping from V1=&gt;KJV and KJV=&gt;V2 gives you Gen.1.1=&gt;Gen.1.1=&gt;Gen.1.1-Gen.1.2 which is inaccurate if in fact
 * V1(Gen.1.1) actually equals V2(Gen.1.1). So instead, we use a split on the right hand-side:
 * </p>
 * <p>
 * For example,<br>
 * V1 defines Gen.1.1=Gen1.1!a, Gen.1.2=Gen.1.1!b<br>
 * V2 defines Gen.1.1=Gen1.1!a, Gen.1.2=Gen.1.1!b<br>
 * then, mapping from V1=&gt;KJV and KJV=&gt;V2 gives you Gen.1.1=&gt;Gen.1.1!a=&gt;Gen.1.1, which is now accurate.
 * A part is a string fragment placed after the end of a key reference. We cannot use # because that is commonly known
 * as a comment in real properties-file. Using a marker, means we can have meaningful part names if we so choose.
 * Parts of ranges are not supported.
 * </p>
 * <p>
 * Note: splits should never be seen by a user. The mapping from one versification to another is abstracted
 * such that the user can simply request the mapping between 2 verse (ranges).
 * </p>
 * <p>
 * Unmapped verses can be specified by inventing ids, either for whole sections, or verse by verse (this would
 * come in handy if two versifications have the same content, but the KJV doesn't). A section must be preceded
 * with a '?' character indicating that there will be no need to try and look up a reference.
 * Gen.1.1=?NewPassage
 * </p>
 * <p>
 * Since not specifying a verse mappings means there is a 1-2-1 unchanged mapping, we need a way of specifying
 * absent verses altogether:<br>
 * ?=Gen.1.1<br>
 * ?=Gen.1.5<br>
 * means that the non-KJV book simply does not contain verses Gen.1.1 and Gen.1.5 and therefore can't
 * be mapped.
 * </p>
 * <p>
 * We allow some global flags (one at present):<br>
 * !zerosUnmapped : means that any mapping to or from a zero verse
 * </p>
 * <p>
 * TODO(CJB): think about whether when returning, we should clone, or make things immutable.
 * </p>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Chris Burrell
 */
open class VersificationToKJVMapper {
    /* the 'from' or 'left' versification */
    private var nonKjv: Versification?

    /* the absent verses, i.e. those present in the KJV, but not in the left versification */
    private var absentVerses: Passage
    private var toKJVMappings: MutableMap<VerseKey<*>, MutableList<QualifiedKey>>
    private var fromKJVMappings: MutableMap<QualifiedKey, Passage>
    private var hasErrors: Boolean = false

    private var osisParser: OsisParser = OsisParser()

    /**
     * @param nonKjv a versification that is not the KJV
     * @param mapping the mappings from one versification to another
     */
    constructor(nonKjv: Versification?, mapping: FileVersificationMapping?) {
        absentVerses = createEmptyPassage(KJV)
        toKJVMappings = HashMap()
        fromKJVMappings = HashMap()
        this.nonKjv = nonKjv
        processMappings(mapping)
        trace()
    }

    /**
     * This is the crux of the decoding facility.  The properties are expanded.
     *
     * @param mappings the input mappings, in a contracted, short-hand form
     */
    private fun processMappings(mappings: FileVersificationMapping?) {
        val entries = mappings!!.mappings
        for (entry in entries) {
            try {
                processEntry(entry)
            } catch (ex: NoSuchKeyException) {
                // TODO(CJB): should we throw a config exception?
                LOGGER.error("Unable to process entry [{}] with value [{}]", entry.key, entry.value, ex)
                hasErrors = true
            }
        }
    }

    @Throws(NoSuchKeyException::class)
    private fun processEntry(entry: KeyValuePair) {
        val leftHand = entry.key
        val kjvHand = entry.value

        if (leftHand == null || leftHand.length == 0) {
            LOGGER.error("Left hand must have content")
            return
        }

        // we allow some global flags properties - for a want of a better syntax!
        // TODO(DMS): remove this from the mapping files
        if ("!zerosUnmapped" == leftHand) {
            return
        }

        // At this point, the left hand side must be a Verse or a VerseRange
        // It cannot be prefixed by +, ?, or !.
        val left = getRange(this.nonKjv, leftHand, null)

        // The right hand side can start with ? which means that the left maps to nothing in the KJV.
        // The ? leads a section name
        val kjv = getRange(KJV, kjvHand, left.key)
        addMappings(left, kjv)
    }

    /**
     * Adds a 1-Many mapping, by simply listing out all the properties. There is probably
     * a better way for storing this, perhaps in a tree - but for simplicity, we're going to list them out.
     *
     * @param leftHand  the left hand side operand
     * @param kjvVerses the verses that are mapped by the left hand side
     */
    @Throws(NoSuchVerseException::class)
    private fun addMappings(leftHand: QualifiedKey, kjvVerses: QualifiedKey) {
        if (leftHand.absentType === QualifiedKey.Qualifier.ABSENT_IN_LEFT) {
            this.absentVerses.addAll(kjvVerses.key)
        } else if (leftHand.key!!.getCardinality() == 1) {
            add1ToManyMappings(leftHand.verse, kjvVerses)
        } else {
            addManyToMany(leftHand, kjvVerses)
        }
    }

    /**
     * Adds a many to many mapping, mappings all the verses on the left hand side to all the verses on the right hand side.
     * We support 2 types: Many-to-1 and Many-to-Many.
     *
     * @param leftHand is assumed to be many
     * @param kjvVerses could be 1 or many
     */
    private fun addManyToMany(leftHand: QualifiedKey, kjvVerses: QualifiedKey) {
        val leftKeys = leftHand.key!!
        val kjvKeys = kjvVerses.key
        val leftIter = leftKeys.iterator()

        if (kjvKeys != null && kjvKeys.getCardinality() != 1) {
            // We detect if the keys are 1-apart from each other. If so, then we skip verse 0 on both sides.
            val diff = Math.abs(leftKeys.getCardinality() - kjvKeys.getCardinality())

            if (diff > 1) {
                reportCardinalityError(leftKeys, kjvKeys)
            }
            val skipVerse0 = diff == 1

            val kjvIter = kjvKeys.iterator()
            while (leftIter.hasNext()) {
                var leftVerse = leftIter.next() as Verse

                // hasNext() and next() have to be paired
                if (!kjvIter.hasNext()) {
                    reportCardinalityError(leftKeys, kjvKeys)
                }

                var rightVerse = kjvIter.next() as Verse
                var kjvKey = QualifiedKey(rightVerse)

                // When the lists are of lengths differing by one
                // it is because 0 is extra.
                // When this is encountered on the left side,
                // we map it and the next verse to the current
                // right verse.
                // In this block we'll handle the case of mapping 0
                // and at the end of the loop we'll handle the next verse.
                if (skipVerse0 && leftVerse.verse == 0) {

                    addForwardMappingFromSingleKeyToRange(leftVerse, kjvKey)
                    addKJVToMapping(kjvKey, leftVerse)

                    if (!leftIter.hasNext()) {
                        reportCardinalityError(leftKeys, kjvKeys)
                    }

                    leftVerse = leftIter.next() as Verse
                }

                // Likewise for the right side,
                // we map it and the next verse to the current
                // left verse.
                // Likewise for this block, it only handles mapping 0
                // Code at the end will map the following verse
                if (skipVerse0 && rightVerse.verse == 0) {

                    addForwardMappingFromSingleKeyToRange(leftVerse, kjvKey)
                    addKJVToMapping(kjvKey, leftVerse)

                    if (!kjvIter.hasNext()) {
                        reportCardinalityError(leftKeys, kjvKeys)
                    }

                    rightVerse = kjvIter.next() as Verse
                    kjvKey = QualifiedKey(rightVerse)
                }

                // Now do the normal case mapping
                addForwardMappingFromSingleKeyToRange(leftVerse, kjvKey)
                addKJVToMapping(kjvKey, leftVerse)
            }

            // Check to see if, having exhausted left that there is more
            // on the right
            if (kjvIter.hasNext()) {
                reportCardinalityError(leftKeys, kjvKeys)
            }
        } else {
            while (leftIter.hasNext()) {
                val leftKey = leftIter.next() as Verse
                addForwardMappingFromSingleKeyToRange(leftKey, kjvVerses)
                addKJVToMapping(kjvVerses, leftKey)
            }
        }

    }

    /**
     * If for some reason cardinalities of keys are different, we report it.
     *
     * @param leftKeys  the left hand key
     * @param kjvKeys the kjv qualified key
     */
    private fun reportCardinalityError(leftKeys: VerseKey<*>, kjvKeys: VerseKey<*>) {
        // TODO (CJB): change this to a neater exception
        // then something went wrong, as we have remaining verses
        throw LucidRuntimeException(String.format("%s has a cardinality of %s whilst %s has a cardinality of %s.",
                leftKeys, Integer.toString(leftKeys.getCardinality()),
                kjvKeys, Integer.toString(kjvKeys.getCardinality())))
    }

    /**
     * If leftKey is non-null (i.e. not attached to a simple specifier, then adds to the kjvTo mappings
     *
     * @param kjvVerses the kjv verses
     * @param leftKey   the left-hand key, possibly null.
     */
    private fun addKJVToMapping(kjvVerses: QualifiedKey, leftKey: Verse?) {
        // NOTE(DMS): Both kjvVerses and left key are each a single verse
        if (leftKey != null) {
            getNonEmptyKey(this.fromKJVMappings, kjvVerses).addAll(leftKey)

            // If we have a part, then we need to add the whole verse as well...
            if (!kjvVerses.isWhole) {
                getNonEmptyKey(this.fromKJVMappings, QualifiedKey.create(kjvVerses.key!!.whole)).addAll(leftKey)
            }
        }
    }

    /**
     * A simple two way entry between 2 1-1 entries.
     *
     * @param leftHand the verse on the left side, left is assumed to be 1 verse only
     * @param kjvHand  the KJV reference
     * @throws NoSuchVerseException
     */
    @Throws(NoSuchVerseException::class)
    private fun add1ToManyMappings(leftHand: Verse?, kjvHand: QualifiedKey) {
        addForwardMappingFromSingleKeyToRange(leftHand, kjvHand)
        addReverse1ToManyMappings(leftHand, kjvHand)
    }

    /**
     * Adds the data into the reverse mappings. Caters for 1-2-1 and 1-2-Many mappings
     *
     * @param leftHand the reference of the left hand reference
     * @param kjvHand  the kjv reference/key, qualified with the part
     */
    private fun addReverse1ToManyMappings(leftHand: Verse?, kjvHand: QualifiedKey) {
        //add the reverse mapping, for 1-1 mappings
        if (kjvHand.absentType === QualifiedKey.Qualifier.ABSENT_IN_KJV || kjvHand.key!!.getCardinality() == 1) {
            // TODO(CJB): deal with parts
            addKJVToMapping(kjvHand, leftHand)
        } else {
            //add the 1-many mappings
            //expand the key and add them all
            //Parts are not supported on ranges...
            val kjvKeys = kjvHand.key!!.iterator()
            while (kjvKeys.hasNext()) {
                addKJVToMapping(QualifiedKey(KeyUtil.getVerse(kjvKeys.next())), leftHand)
            }
        }
    }

    /**
     * Adds a forward mappings from left to KJV.
     *
     * @param leftHand the left hand reference (corresponding to a non-kjv versification)
     * @param kjvHand  the kjv reference (with part if applicable).
     */
    private fun addForwardMappingFromSingleKeyToRange(leftHand: Verse?, kjvHand: QualifiedKey) {
        if (leftHand == null) {
            return
        }

        getNonEmptyMappings(this.toKJVMappings, leftHand).add(kjvHand)
    }

    /**
     * Gets a non-empty key list, either new or the one existing in the map already.
     *
     * @param mappings the map from key to list of values
     * @param key      the key
     * @return the non-empty mappings list
     */
    private fun getNonEmptyKey(mappings: MutableMap<QualifiedKey, Passage>, key: QualifiedKey): VerseKey<*> {
        var matchingVerses = mappings[key]
        if (matchingVerses == null) {
            matchingVerses = createEmptyPassage(this.nonKjv)
            mappings[key] = matchingVerses
        }
        return matchingVerses
    }

    /**
     * Gets a non-empty list, either new or the one existing in the map already
     *
     * @param mappings the map from key to list of values
     * @param key      the key
     * @param <T>      the type of the key
     * @param <S>      the type of the value
     * @return the separate list of verses
     */
    private fun <T, S> getNonEmptyMappings(mappings: MutableMap<T, MutableList<S>>, key: T): MutableList<S> {
        var matchingVerses = mappings[key]
        if (matchingVerses == null) {
            matchingVerses = ArrayList()
            mappings[key] = matchingVerses
        }
        return matchingVerses
    }

    /**
     * Expands a reference to all its verses
     *
     * @param versesKey the verses
     * @return the separate list of verses
     */
    @Throws(NoSuchKeyException::class)
    private fun getRange(versification: Versification?, versesKey: String?, offsetBasis: VerseKey<*>?): QualifiedKey {
        //deal with absent keys in left & absent keys in right, which are simply marked by a '?'
        if (versesKey == null || versesKey.length == 0) {
            throw NoSuchKeyException(JSMsg.gettext("Cannot understand [{0}] as a chapter or verse.", versesKey))
        }

        val firstChar = versesKey[0]
        when (firstChar) {
            '?' ->
                // TODO(CJB): The class JavaDoc has ? on the left side
                // Where is that in any of the mapping code, other than in tests?
                // NOTE(DMS): Does not return a range of any kind.
                return getAbsentQualifiedKey(versification, versesKey)
            '+', '-' ->
                // TODO(CJB): Is + or - allowed only on the right hand side
                // NOTE(DMS): Always returns a QualifiedKey containing a Passage having a VerseRange of 1 or more verses
                return getOffsetQualifiedKey(versification, versesKey, offsetBasis)
            else -> return getExistingQualifiedKey(versification, versesKey)
        }
    }

    /**
     * Deals with offset markers, indicating a passage is +x or -x verses from this one.
     *
     * @param versification the versification of the passed in key
     * @param versesKey     the text of the reference we are trying to parse
     * @return the qualified key representing this
     */
    @Throws(NoSuchKeyException::class)
    private fun getOffsetQualifiedKey(versification: Versification?, versesKey: String, offsetBasis: VerseKey<*>?): QualifiedKey {
        if (offsetBasis == null || offsetBasis.getCardinality() == 0) {
            // TODO(CJB): internationalize
            throw NoSuchKeyException(JSMsg.gettext("Unable to offset the given key [{0}]", offsetBasis))
        }
        val offset = Integer.parseInt(versesKey.substring(1))

        // Convert key immediately to the target versification system, namely the KJV, since it is the only
        // one supported. Convert by ref - since the whole purpose of this is to define equivalents.

        var vr: VerseRange? = null
        if (offsetBasis is VerseRange) {
            vr = offsetBasis
        } else if (offsetBasis is Passage) {
            val iter = offsetBasis.rangeIterator(RestrictionType.NONE)
            if (iter.hasNext()) {
                vr = iter.next() as VerseRange
            }
        }
        if (vr == null) {
            // TODO(CJB): internationalize
            throw NoSuchKeyException(JSMsg.gettext("Unable to offset the given key [{0}]", offsetBasis))
        }

        val vrStart = vr.start
        var start = vrStart.reversify(versification)
        // While you can add a negative number, these are optimized for their operation
        if (offset < 0) {
            start = versification!!.subtract(start, -offset)
        } else if (offset > 0) {
            start = versification!!.add(start, offset)
        }
        var end = start
        if (vr.getCardinality() > 1) {
            end = versification!!.add(start, vr.getCardinality() - 1)
        }

        if (start == null || end == null) {
            hasErrors = true
            LOGGER.error("Verse range with offset did not map to correct range in target versification. This mapping will be set to an empty unmapped key.")
        }

        return if (start != null && end != null) QualifiedKey(VerseRange(versification, start, end)) else QualifiedKey(versesKey)
    }

    /**
     * Deals with real keys found in the versification.
     *
     * @param versification the versification of the passed in key
     * @param versesKey     the text of the reference we are trying to parse
     * @return the qualified key representing this
     */
    private fun getExistingQualifiedKey(versification: Versification?, versesKey: String): QualifiedKey {
        return QualifiedKey(osisParser.parseOsisRef(versification, versesKey))
    }

    /**
     * Deals with absent markers, whether absent in the KJV or absent in the current versification.
     *
     * @param versification the versification of the passed in key
     * @param versesKey     the text of the reference we are trying to parse
     * @return the qualified key representing this
     */
    private fun getAbsentQualifiedKey(versification: Versification?, versesKey: String): QualifiedKey {
        if (versification == this.nonKjv) {
            // we're dealing with a '?', and therefore an ABSENT_IN_LEFT scenarios.
            // we do not support any other ? markers on the left
            return QualifiedKey()
        }
        // we're dealing with a ? on the KJV side, therefore we must be looking at
        // a section name
        return QualifiedKey(versesKey)
    }

    /**
     * @return the qualified keys associated with the input key.
     */
    private fun getQualifiedKeys(leftKey: VerseKey<*>?): MutableList<QualifiedKey>? {
        return this.toKJVMappings[leftKey]
    }

    /**
     * Maps the full qualified key to its proper equivalent in the KJV.
     *
     * @param qualifiedKey the qualified key
     * @return the list of matching qualified keys in the KJV versification.
     */
    open fun map(qualifiedKey: QualifiedKey?): MutableList<QualifiedKey> {
        val key = qualifiedKey!!.key
        if (key is Verse) {
            var kjvKeys = this.getQualifiedKeys(key)
            if (kjvKeys != null && !kjvKeys.isEmpty()) {
                // Found an explicit mapping to KJV, return it.
                return kjvKeys
            } else if (key.isValidIn(KJV)) {
                // No explicit mapping, but key is valid in KJV : implicit straight mapping.
                // Safe to reversify.
                kjvKeys = ArrayList()
                kjvKeys.add(qualifiedKey.reversify(KJV))
                return kjvKeys
            }
            // Else, verses just don't map to KJV : return the empty list.
            return ArrayList()
        }

        return ArrayList()
    }

    /**
     * Converts a KJV verse to the target versification, from a qualified key, rather than a real key
     *
     * @param kjvVerse the verse from the KJV
     * @return the key in the left-hand versification
     */
    open fun unmap(kjvVerse: QualifiedKey?): VerseKey<*>? {
        // TODO(CJB): cope for parts?
        var left = this.fromKJVMappings[kjvVerse]

        if (left == null && !kjvVerse!!.isWhole) {
            // Try again, but without the part this time
            left = this.fromKJVMappings[QualifiedKey(kjvVerse.verse!!.whole)]
        }

        if (left == null) {
            // We didn't find a mapped passage in the left (non-KJV) versification.
            val vk = kjvVerse!!.key
            if (vk != null && this.absentVerses.contains(vk)) {
                // If the verse was explicitly listed as absent, return an empty passage.
                return createEmptyPassage(KJV)
            } else if (vk != null && vk.isValidIn(this.nonKjv)) {
                // If the verse is not explicitly absent, and exists in the left versification,
                // then it's a straight mapping, safe to cast to reversify.
                return kjvVerse.reversify(this.nonKjv).key
            } else {
                // No explicit mapping, and invalid direct cast : these KJV verses do not map to anything.
                return createEmptyPassage(KJV)
            }
        }
        return left
    }

    /**
     * Outputs the mappings for debug purposes to the log file.
     */
    private fun trace() {
        if (LOGGER.isTraceEnabled) {
            val output = StringBuilder()
            dump(output)
            LOGGER.trace(output.toString())
        }
    }

    /**
     * Dump a debug representation of this map to the output stream.
     * 
     * @param out
     */
    open fun dump(out: Appendable) {
        val nonKjvName = this.nonKjv!!.name
        out.appendLine("##############################")
        out.append("Mapping between KJV and $nonKjvName")
        out.appendLine("##############################")
        out.appendLine("    ******************************")
        out.appendLine("    Forward mappings towards KJV")
        out.appendLine("    ******************************")
        for (entry in this.toKJVMappings.entries) {
            val kjvVerses = entry.value
            val osisRef = entry.key.getOsisRef()
            for (q in kjvVerses) {
                out.appendLine("\t($nonKjvName) $osisRef => (KJV) $q")
            }
        }

        out.appendLine("    ******************************")
        out.appendLine("    Absent verses in left versification:")
        out.appendLine("\t[${this.absentVerses.getOsisRef()}]")
        out.appendLine("    ******************************")
        out.appendLine("    Backwards mappings from KJV")
        out.appendLine("    ******************************")
        for (entry in this.fromKJVMappings.entries) {
            out.appendLine("\t(KJV): ${entry.key} => ($nonKjvName) ${entry.value.getOsisRef()}")
        }
    }

    /**
     * Returns whether we initialised with errors
     * Java had this method package-private (was package-private).
     */
    open fun hasErrors(): Boolean {
        return hasErrors
    }

    /** Simplify creation of an empty passage object of the default type, with the required v11n.
     * 
     * @param versification required v11n for new Passage
     * @return              empty Passage
     */
    private fun createEmptyPassage(versification: Versification?): Passage {
        return RangedPassage(versification)
    }

    private companion object {
        private val KJV = Versifications.instance().getVersification(SystemKJVA.V11N_NAME)!!
        private val LOGGER: Logger = LoggerFactory.getLogger(VersificationToKJVMapper::class)
    }
}
