
package org.crosswire.jsword.book.search.parse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.BookUtil;
import org.crosswire.jsword.book.Search;
import org.crosswire.jsword.book.search.Index;
import org.crosswire.jsword.book.search.Parser;
import org.crosswire.jsword.passage.Passage;
import org.crosswire.jsword.passage.PassageConstants;
import org.crosswire.jsword.passage.PassageFactory;
import org.crosswire.jsword.passage.PassageTally;

/**
 * The central interface to all searching.
 * 
 * Functionality the I invisage includes:<ul>
 * <li>A simple search syntax that goes something like this.<ul>
 *     <li>aaron, moses     (verses containing aaron and moses. Can also use & or +)
 *     <li>aaron/moses      (verses containing aaron or moses. Can also use |)
 *     <li>aaron - moses    (verses containing aaron but not moses)
 *     <li>aaron ~5 , moses (verses with aaron within 5 verses of moses)
 *     <li>soundslike aaron (verses with words that sound like aaron. Can also use sl ...)
 *     <li>thesaurus happy  (verses with words that mean happy. Can also use th ...)
 *     <li>grammar have     (words like has have had and so on. Can also use gr ...)</ul>
 * <li>The ability to add soundslike type extensions.</ul>
 * 
 * <p><table border='1' cellPadding='3' cellSpacing='0'>
 * <tr><td bgColor='white' class='TableRowColor'><font size='-7'>
 *
 * Distribution Licence:<br />
 * JSword is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License,
 * version 2 as published by the Free Software Foundation.<br />
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br />
 * The License is available on the internet
 * <a href='http://www.gnu.org/copyleft/gpl.html'>here</a>, or by writing to:
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA<br />
 * The copyright to this program is held by it's authors.
 * </font></td></tr></table>
 * @see docs.Licence
 * @author Joe Walker [joe at eireneh dot com]
 * @version $Id$
 */
public class LocalParser implements Parser
{
    /**
     * Create a new search engine.
     * @param index The Index to search
     */
    public void init(Index newindex)
    {
        this.index = newindex;
        this.commands = SearchDefault.getMap();
    }

    /**
     * Take a search string and decipher it into a Passage.
     * @param sought The string to be searched for
     * @return The matching verses
     */
    public Passage search(Search search) throws BookException
    {
        Passage ref = null;

        if (search.isBestMatch())
        {
            ref = bestMatch(search.getMatch());
        }
        else
        {
            output = CustomTokenizer.tokenize(search.getMatch(), commands);
            ref = search(output);
        }

        if (search.isRestricted())
        {
            Passage restrict = search.getRestriction();
            ref.retainAll(restrict);
        }

        return ref;
    }

    /**
     * Generate a bestmatch search
     * @param sought
     * @return Passage
     * @throws SearchException
     */
    private Passage bestMatch(String sought) throws BookException
    {
        String[] words = BookUtil.getWords(sought);
        words = Grammar.stripSmallWords(words);
        // log.fine("words="+StringUtil.toString(words));
        
        PassageTally tally = new PassageTally();
        tally.blur(2, PassageConstants.RESTRICT_NONE);

        for (int i=0; i<words.length; i++)
        {
            tally.addAll(wordSearch(words[i]));
        }
    
        // This uses updatePassageTallyFlat() so that words like God
        // that have many startsWith() matches, and hence many verse
        // matches, do not end up with wrongly high scores.
        for (int i=0; i<words.length; i++)
        {
            // log.fine("considering="+words[i]);
            String root = Grammar.getRoot(words[i]);
    
            // Check that the root is still a word. If not then we
            // use the full version. This catches misses like se is
            // the root of seed, and matches sea and so on ...
            Passage ref = wordSearch(root);
            if (ref.isEmpty())
                root = words[i];
    
            // log.fine("  root="+root);
            Iterator it = index.getStartsWith(root);
            String[] gr_words = BookUtil.toStringArray(it);
    
            // log.fine("  gr_words="+StringUtil.toString(gr_words));
            updatePassageTallyFlat(tally, gr_words);
        }
        
        return tally;
    }

    /**
     * Take a search string and decipher it into a Passage.
     * @param ref The Passage to alter
     * @param sought The string to be searched for
     * @return The matching verses
     */
    protected Passage wordSearch(String sought) throws BookException
    {
        return index.findWord(sought);
    }

    /**
     * Take a search string and decipher it into a Passage.
     * @param ref The Passage to alter
     * @param sought The string to be searched for
     * @return The matching verses
     */
    protected Passage wordSearch(Passage ref, String sought) throws BookException
    {
        output = CustomTokenizer.tokenize(sought, commands);
        return search(ref, output);
    }

    /**
     * Take a search string and decipher it into a Passage.
     * @param output The string to be searched for as a Vector of SearchWords
     * @return The matching verses
     */
    protected Passage search(List matches) throws BookException
    {
        Passage ref = PassageFactory.createPassage();
        return search(ref, matches);
    }

    /**
     * A basic version of getPassage(String[]) simply calls getPassage(String)
     * in a loop for each word, adding the Verses to an Passage that is returned
     * @param version The version to search using
     * @param words The words to search for
     * @return The Passage
     * @throws BookException If anything goes wrong with this method
     */
    protected Passage getPassage(String[] words) throws BookException
    {
        Passage ref = PassageFactory.createPassage();

        for (int i=0; i<words.length; i++)
        {
            ref.addAll(wordSearch(words[i]));
        }

        return ref;
    }

    /**
     * Take a search string and decipher it into a Passage.
     * @param ref The Passage to alter
     * @param output The string to be searched for as a Vector of SearchWords
     * @return The Passage passed in
     */
    protected Passage search(Passage ref, List matches) throws BookException
    {
        // Check that there is a CommandWord first
        if (!(matches.get(0) instanceof CommandWord))
        {
            // Add a default AddCommandWord if not
            matches.add(0, new AddCommandWord());
        }

        wit = matches.iterator();
        while (wit.hasNext())
        {
            Object temp = wit.next();
            try
            {
                CommandWord command = (CommandWord) temp;
                command.updatePassage(this, ref);
            }
            catch (ClassCastException ex)
            {
                throw new BookException(Msg.ENGINE_SYNTAX, new Object[] { temp });
            }
        }

        // Set these to null so that people can't play around
        // with them once they're done with, and to save memory.
        matches = null;
        wit = null;

        return ref;
    }

    /**
     * This is similar to updatePassageTally() however if a verse matches
     * many words it still only adds on for that verse in the given tally
     * @param version The version to search using
     * @param tally The PassageTally to update
     * @param words The words to search for
     * @throws BookException If anything goes wrong with this method
     */
    private void updatePassageTallyFlat(PassageTally tally, String[] words) throws BookException
    {
        PassageTally temp = new PassageTally();

        for (int i=0; i<words.length; i++)
        {
            temp.addAll(wordSearch(words[i]));
        }

        temp.flatten();
        tally.addAll(temp);
    }

    /**
     * Accessor for the Bible to search.
     * @return The current Bible
     */
    protected Index getIndex()
    {
        return index;
    }

    /**
     * Accessor for the available SearchWords. This is probably
     * the same as from Options.getSearchHashtable() but just in
     * case anyone has been playing around with it...
     * @return The SearchWord Hashtable
     */
    protected Map getSearchMap()
    {
        return commands;
    }

    /**
     * Accessor for the available SearchWords. This is probably
     * the same as from Options.getSearchHashtable() but just in
     * case anyone has been playing around with it...
     * @return The SearchWord Hashtable
     */
    protected void setSearchMap(Map commands)
    {
        this.commands = commands;
    }

    /**
     * Most SearchWords (Almost all except the DefaultParamWord) need
     * to access parameters, this method allows them access to the
     * Parser's own Enumerator. Use with care, and only if you are a
     * SearchWord taking part in the current search.
     * @return The current Enumerator
     */
    protected Iterator iterator()
    {
        return wit;
    }

    /** The parsed version of the current string */
    private List output = null;

    /** The commands that we know about */
    private Map commands = null;

    /** While the answer is being worked out ... */
    private Iterator wit = null;

    private Index index;
}