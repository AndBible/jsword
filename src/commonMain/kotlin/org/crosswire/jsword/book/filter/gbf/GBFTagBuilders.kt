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
package org.crosswire.jsword.book.filter.gbf

import kotlin.jvm.JvmStatic


import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.filter.gbf.GBFTags.BoldStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.CrossRefStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.BookTitleStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.DefaultEndTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.EOLTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.FootnoteEndTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.FootnoteStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.HeaderStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.IgnoredTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.ItalicStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.JustifyRightTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.OTQuoteStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.ParagraphTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.PoetryStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.PsalmStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.RedLetterStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.StrongsMorphTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.StrongsWordTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.TextFootnoteTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.TextTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.TitleStartTag
import org.crosswire.jsword.book.filter.gbf.GBFTags.UnderlineStartTag
import org.crosswire.jsword.passage.Key
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * This class is a convenience to get GBF Tags.
 * 
 * The best place to go for more information about the GBF spec that I have
 * found is: <a
 * href="http://ebible.org/bible/gbf.htm">http://ebible.org/bible/gbf.htm</a>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class GBFTagBuilders private constructor() {
        /**
         *
         */
        internal class BoldStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return BoldStartTag(name)
            }
        }

        /**
         *
         */
        internal class BookTitleStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return BookTitleStartTag(name)
            }
        }

        /**
         *
         */
        internal class CrossRefStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return CrossRefStartTag(name)
            }
        }

        /**
         *
         */
        internal class DefaultEndTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return DefaultEndTag(name)
            }
        }

        /**
         *
         */
        internal class EndOfLineTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return EOLTag(name)
            }
        }

        /**
         *
         */
        internal class EscapeTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            if ("CG" == name) {
                return TextTag("&gt;")
            }

            // else "CT"
            return TextTag("&lt;")
            }
        }

        /**
         *
         */
        internal class FootnoteStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return FootnoteStartTag(name)
            }
        }

        /**
         *
         */
        internal class FootnoteEndTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return FootnoteEndTag(name)
            }
        }

        /**
         *
         */
        internal class HeaderStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return HeaderStartTag(name)
            }
        }

        /**
         *
         */
        internal class IgnoredTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return IgnoredTag(name)
            }
        }

        /**
         *
         */
        internal class ItalicStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return ItalicStartTag(name)
            }
        }

        /**
         *
         */
        internal class JustifyRightTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return JustifyRightTag(name)
            }
        }

        /**
         *
         */
        internal class OTQuoteStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return OTQuoteStartTag(name)
            }
        }

        /**
         *
         */
        internal class ParagraphTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return ParagraphTag(name)
            }
        }

        /**
         *
         */
        internal class PoetryStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return PoetryStartTag(name)
            }
        }

        /**
         *
         */
        internal class PsalmTitleStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return PsalmStartTag(name)
            }
        }

        /**
         *
         */
        internal class RedLetterStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return RedLetterStartTag(name)
            }
        }

        /**
         *
         */
        internal class StrongsMorphTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return StrongsMorphTag(name)
            }
        }

        /**
         *
         */
        internal class StrongsWordTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return StrongsWordTag(name)
            }
        }

        /**
         *
         */
        internal class TextFootnoteTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return TextFootnoteTag(name)
            }
        }

        /**
         *
         */
        internal class TitleStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return TitleStartTag(name)
            }
        }

        /**
         *
         */
        internal class UnderlineStartTagBuilder : TagBuilder {
            override fun createTag(name: String): Tag? {
            return UnderlineStartTag(name)
            }
        }

    companion object {
        /**
         * @param book the book
         * @param key the key
         * @param name the tag name
         * @return return a GBF Tag for the given tag name
         */
        @JvmStatic
        fun getTag(book: Book?, key: Key?, name: String): Tag? {
            var tag: Tag? = null
            if (name.startsWith("W") && (name.contains("-") || name.contains(":")) && name.matches(Regex("WT?[GH] ?[0-9]+[-:][0-9abc-]+"))) {
                // these tags show verse boundaries in different versification;
                // ignore them instead of parsing them as Strongs / Morphology tags
                return null
            }
            val length = name.length
            if (length > 0) {
                // Only the first two letters of the tag are indicative of the tag
                // The rest, if present, is data.
                var builder: TagBuilder? = null
                if (length == 2) {
                    builder = BUILDERS.get(name)
                } else if (length > 2) {
                    builder = BUILDERS.get(name.substring(0, 2))
                }

                if (builder != null) {
                    tag = builder.createTag(name)
                }

                if (tag == null) {
                    // I'm not confident enough that we handle all the GBF tags
                    // that I will blame the book instead of the program
                    log.warn("In {}({}) ignoring tag of <{}>", book!!.initials, key!!.getName(), name)
                }
            }
            return tag
        }

        /**
         * @param text
         * @return get a Text Tag object containing the text
         */
        @JvmStatic
        fun getTextTag(text: String): Tag {
            return TextTag(text)
        }

        /**
         * The <code>BUILDERS</code> maps the 2 letter GBF tag to a class that
         * proxies for the tag.
         */
        private val BUILDERS: MutableMap<String, TagBuilder> = HashMap()

        init {
            val defaultEndTagBuilder: TagBuilder = DefaultEndTagBuilder()
            val ignoreTagBuilder: TagBuilder = IgnoredTagBuilder()

            BUILDERS.put("FB", BoldStartTagBuilder())
            BUILDERS.put("Fb", defaultEndTagBuilder)

            BUILDERS.put("FI", ItalicStartTagBuilder())
            BUILDERS.put("Fi", defaultEndTagBuilder)

            BUILDERS.put("FR", RedLetterStartTagBuilder())
            BUILDERS.put("Fr", defaultEndTagBuilder)

            BUILDERS.put("FU", UnderlineStartTagBuilder())
            BUILDERS.put("Fu", defaultEndTagBuilder)

            BUILDERS.put("RX", CrossRefStartTagBuilder())
            BUILDERS.put("Rx", defaultEndTagBuilder)

            BUILDERS.put("CL", EndOfLineTagBuilder())
            BUILDERS.put("CM", ParagraphTagBuilder())

            BUILDERS.put("RF", FootnoteStartTagBuilder())
            BUILDERS.put("Rf", FootnoteEndTagBuilder())
            BUILDERS.put("RB", TextFootnoteTagBuilder())

            BUILDERS.put("TS", HeaderStartTagBuilder())
            BUILDERS.put("Ts", defaultEndTagBuilder)

            BUILDERS.put("TB", PsalmTitleStartTagBuilder())
            BUILDERS.put("Tb", defaultEndTagBuilder)

            BUILDERS.put("TH", TitleStartTagBuilder())
            BUILDERS.put("Th", defaultEndTagBuilder)

            BUILDERS.put("TT", BookTitleStartTagBuilder())
            BUILDERS.put("Tt", defaultEndTagBuilder)

            BUILDERS.put("BA", ignoreTagBuilder)
            BUILDERS.put("BC", ignoreTagBuilder)
            BUILDERS.put("BI", ignoreTagBuilder)
            BUILDERS.put("BN", ignoreTagBuilder)
            BUILDERS.put("BO", ignoreTagBuilder)
            BUILDERS.put("BP", ignoreTagBuilder)

            BUILDERS.put("JR", JustifyRightTagBuilder())
            BUILDERS.put("JC", ignoreTagBuilder)
            BUILDERS.put("JL", ignoreTagBuilder)

            BUILDERS.put("FO", OTQuoteStartTagBuilder())
            BUILDERS.put("Fo", defaultEndTagBuilder)

            BUILDERS.put("PP", PoetryStartTagBuilder())
            BUILDERS.put("Pp", defaultEndTagBuilder)

            val builder: TagBuilder = StrongsWordTagBuilder()
            BUILDERS.put("WH", builder)
            BUILDERS.put("WG", builder)
            BUILDERS.put("WT", StrongsMorphTagBuilder())

            BUILDERS.put("CG", EscapeTagBuilder())
            BUILDERS.put("CT", EscapeTagBuilder())
        }

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(GBFTagBuilders::class)
    }
}
