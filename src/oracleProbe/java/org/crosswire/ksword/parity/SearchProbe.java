package org.crosswire.ksword.parity;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.crosswire.common.util.CWProject;
import org.crosswire.common.util.Language;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.OSISUtil;
import org.crosswire.jsword.book.sword.SwordBookPath;
import org.crosswire.jsword.index.IndexPolicy;
import org.crosswire.jsword.index.lucene.LuceneIndex;
import org.crosswire.jsword.index.lucene.analysis.AbstractBookAnalyzer;
import org.crosswire.jsword.index.lucene.analysis.AnalyzerFactory;
import org.crosswire.jsword.index.lucene.analysis.SimpleLuceneAnalyzer;
import org.crosswire.jsword.index.lucene.analysis.StrongsNumberFilter;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.Verse;

/**
 * Search parity ops (KSword K6), compiled only against the frozen baseline (jsword-baseline.jar plus its Lucene 3.6.2
 * third-party jars); never part of the current code. Uses only baseline API. Failures are returned as
 * "!" + exception class + ": " + message.
 *
 * Ops: analyze, analyzeBook, canonicalText, buildIndex, find, stem, version (see k6 plan, Task 0), strongs (Task 8:
 * the baseline StrongsNumberFilter over whitespace tokens).
 */
public final class SearchProbe implements BiFunction<String, List<String>, String> {
    private static final String LUCENE_VERSION = "3.6.2";

    /** Mirrors the index policy of the app at the time of the baseline: Strong's numbers only, built serially. */
    private static final class Policy implements IndexPolicy {
        public boolean isStrongsIndexed() { return true; }
        public boolean isMorphIndexed() { return false; }
        public boolean isNoteIndexed() { return false; }
        public boolean isTitleIndexed() { return false; }
        public boolean isXrefIndexed() { return false; }
        public int getRAMBufferSize() { return 16; }
        public boolean isSerial() { return true; }
    }

    @Override
    public String apply(String op, List<String> args) {
        try {
            switch (op) {
                case "analyze": return analyze(args.get(0), args.get(1));
                case "analyzeBook": return analyzeBook(args.get(0), args.get(1), Integer.parseInt(args.get(2)));
                case "canonicalText": return canonicalText(args.get(0), args.get(1), Integer.parseInt(args.get(2)));
                case "buildIndex": return buildIndex(args.get(0), args.get(1), args.get(2));
                case "find": return find(args.get(0), args.get(1), args.get(2), args.get(3));
                case "search": return search(args.get(0), args.get(1), args.get(2), args.get(3));
                case "stem": return stem(args.get(0), args.subList(1, args.size()));
                case "version": return version();
                case "strongs": return strongs(args.get(0));
                default: return "!IllegalArgumentException: unknown op " + op;
            }
        } catch (Throwable t) {
            return "!" + t.getClass().getName() + ": " + t.getMessage()
                + (t.getCause() != null ? " <- " + t.getCause().getClass().getName() : "");
        }
    }

    /** The baseline StrongsNumberFilter over WhitespaceTokenizer (the strong field analyzer, no book), one token per line. */
    private static String strongs(String text) throws Exception {
        TokenStream ts = new StrongsNumberFilter(new WhitespaceTokenizer(new StringReader(text)));
        CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
        StringBuilder out = new StringBuilder();
        ts.reset();
        boolean first = true;
        while (ts.incrementToken()) {
            if (!first) out.append('\n');
            first = false;
            out.append(term.toString());
        }
        ts.end();
        ts.close();
        return out.toString();
    }

    private static String version() throws Exception {
        String commit = "unknown";
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarOf(Books.class))) {
            String c = jar.getManifest().getMainAttributes().getValue("Baseline-Commit");
            if (c != null) commit = c;
        }
        return commit + "\nlucene " + LUCENE_VERSION;
    }

    private static File jarOf(Class<?> c) throws Exception {
        return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    /** The analyzer the baseline would create for a book of this language, with its factory defaults, no book (Snowball: stemmer picked from the code). */
    private static AbstractBookAnalyzer analyzerFor(String lang) throws Exception {
        AnalyzerFactory factory = AnalyzerFactory.getInstance();
        String className = factory.getAnalyzerValue(new Language(lang));
        AbstractBookAnalyzer analyzer;
        if (className == null) {
            // As AnalyzerFactory.createAnalyzer: no configured analyzer for the language falls back to the simple one.
            analyzer = new SimpleLuceneAnalyzer();
        } else {
            try {
                analyzer = (AbstractBookAnalyzer) Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot construct " + className, e);
            }
        }
        if (analyzer instanceof org.crosswire.jsword.index.lucene.analysis.ConfigurableSnowballAnalyzer) {
            // The stemmer is picked by setBook from the book language; with no book, pick it from the same code.
            ((org.crosswire.jsword.index.lucene.analysis.ConfigurableSnowballAnalyzer) analyzer).pickStemmer(new Language(lang).getCode());
        }
        analyzer.setDoStemming(factory.getDefaultStemmingProperty());
        analyzer.setDoStopWords(factory.getDefaultStopWordProperty());
        return analyzer;
    }

    private static String analyze(String lang, String text) throws Exception {
        AbstractBookAnalyzer analyzer = analyzerFor(lang);
        StringBuilder out = new StringBuilder();
        TokenStream ts = analyzer.tokenStream(LuceneIndex.FIELD_BODY, new StringReader(text));
        CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute incr = ts.addAttribute(PositionIncrementAttribute.class);
        ts.reset();
        boolean first = true;
        while (ts.incrementToken()) {
            if (!first) out.append('\n');
            first = false;
            out.append(term.toString()).append('\t').append(incr.getPositionIncrement());
        }
        ts.end();
        ts.close();
        return out.toString();
    }

    private static boolean homeSet;

    private static synchronized Book openBook(String corpusRoot, String initials) throws Exception {
        if (!homeSet) {
            // The baseline's InstalledIndex writes its metadata under the project home (relative to user.home) and
            // needs its "lucene" subdirectory to exist; keep that out of ~/.jsword.
            CWProject.setHome("jsword.home", ".ksword-k6-oracle", null);
            CWProject.instance().getWritableProjectSubdir("lucene", true);
            homeSet = true;
        }
        SwordBookPath.setAugmentPath(new File[] {new File(corpusRoot)});
        Book book = Books.installed().getBook(initials);
        if (book == null) throw new IllegalStateException("no such book " + initials + " in " + corpusRoot);
        return book;
    }

    private static boolean isVerseZero(Key key) {
        return key instanceof Verse && ((Verse) key).getVerse() == 0;
    }

    private static String analyzeBook(String corpusRoot, String initials, int stride) throws Exception {
        Book book = openBook(corpusRoot, initials);
        AbstractBookAnalyzer analyzer = AnalyzerFactory.getInstance().createAnalyzer(book);
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (Key key : book.getGlobalKeyList()) {
            if (isVerseZero(key)) continue;
            if (n++ % stride != 0) continue;
            String text = OSISUtil.getCanonicalText(new BookData(book, key).getOsisFragment());
            TokenStream ts = analyzer.tokenStream(LuceneIndex.FIELD_BODY, new StringReader(text));
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute incr = ts.addAttribute(PositionIncrementAttribute.class);
            ts.reset();
            StringBuilder line = new StringBuilder(key.getOsisRef()).append('\t');
            boolean first = true;
            while (ts.incrementToken()) {
                if (!first) line.append(' ');
                first = false;
                line.append(term.toString());
                if (incr.getPositionIncrement() != 1) line.append('@').append(incr.getPositionIncrement());
            }
            ts.end();
            ts.close();
            out.append(line).append('\n');
        }
        return out.toString();
    }

    /** One line per key (verse 0 skipped, every stride-th): osisRef, a tab, the canonical text with \\, tab, LF, CR escaped. */
    private static String canonicalText(String corpusRoot, String initials, int stride) throws Exception {
        Book book = openBook(corpusRoot, initials);
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (Key key : book.getGlobalKeyList()) {
            if (isVerseZero(key)) continue;
            if (n++ % stride != 0) continue;
            String text = OSISUtil.getCanonicalText(new BookData(book, key).getOsisFragment());
            out.append(key.getOsisRef()).append('\t')
                .append(text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r"))
                .append('\n');
        }
        return out.toString();
    }

    private static String buildIndex(String corpusRoot, String initials, String indexDir) throws Exception {
        Book book = openBook(corpusRoot, initials);
        File dir = new File(indexDir);
        // The baseline builds into "<dir>.CREATING" and renames; renaming onto an existing dir fails, so start clean.
        deleteRecursively(dir);
        try (LuceneIndex index = new LuceneIndex(book, dir.toURI(), new Policy())) {
            int docs = index.getSearcher().maxDoc();
            return "ok " + docs;
        }
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteRecursively(c);
        f.delete();
    }

    private static String find(String corpusRoot, String initials, String indexDir, String query) throws Exception {
        Book book = openBook(corpusRoot, initials);
        // The two-argument constructor opens an existing index; the three-argument one always (re)builds.
        try (LuceneIndex index = new LuceneIndex(book, new File(indexDir).toURI())) {
            Key result = index.find(query);
            List<String> refs = new ArrayList<>();
            for (Key key : result) {
                refs.add(key.getOsisRef());
            }
            Collections.sort(refs);
            StringBuilder out = new StringBuilder();
            for (String ref : refs) out.append(ref).append('\n');
            return out.toString();
        }
    }

    /**
     * The whole `Book.find` path below the book: the JSword query layer (`LuceneSearcher` + `QueryBuilder`: passage
     * ranges `+[Gen-Mal]`, blur, boolean split) over the Lucene index, like `book.find(request)` once the book is indexed.
     */
    private static String search(String corpusRoot, String initials, String indexDir, String query) throws Exception {
        Book book = openBook(corpusRoot, initials);
        try (LuceneIndex index = new LuceneIndex(book, new File(indexDir).toURI())) {
            org.crosswire.jsword.index.lucene.LuceneSearcher searcher = new org.crosswire.jsword.index.lucene.LuceneSearcher();
            searcher.init(index);
            Key result = searcher.search(query);
            List<String> refs = new ArrayList<>();
            for (Key key : result) {
                refs.add(key.getOsisRef());
            }
            Collections.sort(refs);
            StringBuilder out = new StringBuilder();
            for (String ref : refs) out.append(ref).append('\n');
            return out.toString();
        }
    }

    private static String stem(String name, List<String> words) throws Exception {
        Class<?> cls = Class.forName("org.tartarus.snowball.ext." + name + "Stemmer");
        org.tartarus.snowball.SnowballProgram stemmer = (org.tartarus.snowball.SnowballProgram) cls.getDeclaredConstructor().newInstance();
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            stemmer.setCurrent(word);
            stemmer.stem();
            out.append(stemmer.getCurrent()).append('\n');
        }
        return out.toString();
    }
}
