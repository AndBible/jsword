package org.crosswire.ksword.parity;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookMetaData;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.sword.SwordBookMetaData;
import org.crosswire.jsword.passage.BitwisePassage;
import org.crosswire.jsword.passage.DistinctPassage;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.Passage;
import org.crosswire.jsword.passage.PassageKeyFactory;
import org.crosswire.jsword.passage.PassageTally;
import org.crosswire.jsword.passage.RangedPassage;
import org.crosswire.jsword.passage.RestrictionType;
import org.crosswire.jsword.passage.RocketPassage;
import org.crosswire.jsword.passage.Verse;
import org.crosswire.jsword.passage.VerseRange;
import org.crosswire.jsword.versification.BibleBook;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.VersificationsMapper;
import org.crosswire.jsword.versification.system.Versifications;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Corpus parity ops (KSword K1 spec §4.2), compiled against both the baseline and the current code.
 * Uses only API present in the baseline. Failures are returned as "!" + exception class + message so the
 * two sides can be compared even when both throw.
 */
final class CorpusProbe {
    private CorpusProbe() {
    }

    private static void throwTarget(Throwable target) throws Exception {
        if (target instanceof Exception) throw (Exception) target;
        if (target instanceof Error) throw (Error) target;
        CorpusProbe.<RuntimeException>rethrow(target);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(Throwable target) throws T { throw (T) target; }

    private static Object invokeTarget(java.lang.reflect.Method method, Object... args) throws Exception {
        try { return method.invoke(null, args); }
        catch (java.lang.reflect.InvocationTargetException failure) {
            throwTarget(failure.getTargetException());
            throw new AssertionError("unreachable");
        }
    }

    private static SwordBookMetaData fileMetadata(File file, java.net.URI root) throws Exception {
        try {
            Class<?> holder = Class.forName("org.crosswire.jsword.book.sword.SwordBookMetaDataJvm");
            return (SwordBookMetaData) invokeTarget(holder.getMethod("create", File.class, java.net.URI.class), file, root);
        } catch (ClassNotFoundException oldApi) {
            try {
                return SwordBookMetaData.class.getConstructor(File.class, java.net.URI.class).newInstance(file, root);
            } catch (java.lang.reflect.InvocationTargetException failure) {
                throwTarget(failure.getTargetException());
                throw new AssertionError("unreachable");
            }
        }
    }

    private static Class<?> pathBoundary() throws Exception {
        try { return Class.forName("org.crosswire.jsword.book.sword.SwordBookPathJvm"); }
        catch (ClassNotFoundException oldApi) { return Class.forName("org.crosswire.jsword.book.sword.SwordBookPath"); }
    }

    private static File downloadDir() throws Exception {
        return (File) invokeTarget(pathBoundary().getMethod("getSwordDownloadDir"));
    }

    private static void setDownloadDir(File dir) throws Exception {
        invokeTarget(pathBoundary().getMethod("setDownloadDir", File.class), dir);
    }

    static String apply(String op, List<String> args) throws Exception {
        if (op.equals("passage.parseAllFi")) {
            // Same as passage.parseAll, but with the Finnish locale so BibleNames_fi names resolve.
            final java.util.Locale fi = new java.util.Locale("fi", "FI");
            org.crosswire.jsword.internationalisation.LocaleProvider old =
                org.crosswire.jsword.internationalisation.LocaleProviderManager.getLocaleProvider();
            org.crosswire.jsword.internationalisation.LocaleProviderManager.setLocaleProvider(
                (org.crosswire.jsword.internationalisation.LocaleProvider) java.lang.reflect.Proxy.newProxyInstance(
                    org.crosswire.jsword.internationalisation.LocaleProvider.class.getClassLoader(),
                    new Class<?>[] {org.crosswire.jsword.internationalisation.LocaleProvider.class},
                    (proxy, method, arguments) -> {
                        // The baseline returns Locale; K3 returns KLocale. Keep this shared probe source portable.
                        Class<?> localeType = method.getReturnType();
                        if (localeType == java.util.Locale.class) {
                            return fi;
                        }
                        return localeType.getConstructor(String.class, String.class, String.class)
                            .newInstance(fi.getLanguage(), fi.getCountry(), fi.getVariant());
                    }));
            try {
                return apply("passage.parseAll", args);
            } finally {
                org.crosswire.jsword.internationalisation.LocaleProviderManager.setLocaleProvider(old);
            }
        }
        switch (op) {
            case "fixture.dump": {
                File conf = new File(args.get(0));
                SwordBookMetaData md = fileMetadata(conf, conf.getParentFile().getParentFile().toURI());
                Book book;
                if (md.getInitials().equals("K4RawText4")) {
                    if (md.getBookType() != null) {
                        throw new AssertionError("RawText4 discovery unexpectedly supported; baseline has no registration");
                    }
                    book = new org.crosswire.jsword.book.sword.SwordBook(md,
                        new org.crosswire.jsword.book.sword.RawBackend(md, 4));
                } else {
                    book = md.getBookType().createBook(md);
                }
                // AbstractBook exposes getBackend in both versions; reflection avoids binding to facade seams.
                Object backend = book.getClass().getMethod("getBackend").invoke(book);
                java.lang.reflect.Method length = backend.getClass().getMethod("getRawTextLength", Key.class);
                StringBuilder sb = new StringBuilder();
                int i = 0;
                try {
                    for (Key key : book.getGlobalKeyList()) {
                        String raw;
                        try {
                            raw = java.util.Base64.getEncoder().encodeToString(book.getRawText(key).getBytes(StandardCharsets.UTF_8));
                        } catch (Exception e) {
                            raw = fail(e);
                        }
                        sb.append(i++).append('\t').append(key.getName()).append('\t').append(raw)
                          .append('\t').append(length.invoke(backend, key)).append('\n');
                    }
                    sb.append("count=").append(i).append('\n');
                    if (md.getInitials().equals("K4GenBook")) {
                        for (Key key : book.getGlobalKeyList()) {
                            String path = "";
                            for (Key node = key; node != null && !node.getName().isEmpty(); node = node.getParent()) {
                                path = "/" + node.getName() + path;
                            }
                            sb.append("tree\t").append(path).append('\n');
                        }
                    }
                    // RawText4's test-only backend is verse-based despite its unregistered metadata type.
                    if (md.getKeyType() == org.crosswire.jsword.book.KeyType.VERSE
                            || md.getInitials().equals("K4RawText4")) {
                        for (String ref : new String[] {"Gen.1.4", "John.1.4", "Rev.22.21"}) {
                            Key key = book.getKey(ref);
                            sb.append("empty\t").append(ref).append('\t')
                              .append(java.util.Base64.getEncoder().encodeToString(book.getRawText(key).getBytes(StandardCharsets.UTF_8)))
                              .append('\t').append(length.invoke(backend, key)).append('\n');
                        }
                    }
                    if (md.getInitials().equals("K4Devotion")) {
                        for (String internal : new String[] {"01.01", "02.29", "12.31"}) {
                            Key key = new org.crosswire.jsword.passage.DefaultLeafKeyList(internal);
                            String raw = (String) backend.getClass().getMethod("getRawText", Key.class).invoke(backend, key);
                            sb.append("internal\t").append(internal).append('\t')
                              .append(java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)))
                              .append('\t').append(length.invoke(backend, key)).append('\n');
                        }
                    }
                    return sb.toString();
                } finally {
                    book.getClass().getMethod("deactivate", org.crosswire.common.activate.Lock.class).invoke(book, new Object[] {null});
                }
            }
            case "corpus.modules": {
                List<String> names = new ArrayList<>();
                for (Book b : Books.installed().getBooks()) {
                    names.add(b.getInitials());
                }
                Collections.sort(names);
                return String.join("\n", names);
            }
            case "corpus.meta": {
                Book book = book(args.get(0));
                BookMetaData md = book.getBookMetaData();
                StringBuilder sb = new StringBuilder();
                sb.append("name=").append(md.getName()).append('\n');
                sb.append("category=").append(book.getBookCategory()).append('\n');
                sb.append("language=").append(book.getLanguage() == null ? "<null>" : book.getLanguage().getCode()).append('\n');
                dumpMeta(sb, md);
                return sb.toString();
            }
            case "corpus.keys": {
                // args: initials, stride. One line per sampled key: index, osisRef, name, sha(raw), sha(osis)
                Book book = book(args.get(0));
                int stride = Integer.parseInt(args.get(1));
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (Key key : book.getGlobalKeyList()) {
                    if (i % stride == 0) {
                        sb.append(i).append('\t').append(safeOsisRef(key)).append('\t').append(key.getName())
                          .append('\t').append(sha(raw(book, key))).append('\t').append(sha(osis(book, key))).append('\n');
                    }
                    i++;
                }
                sb.append("count=").append(i).append('\n');
                return sb.toString();
            }
            case "corpus.keyAt": {
                // args: initials, index. Full raw and OSIS for one key (mismatch report).
                Book book = book(args.get(0));
                int target = Integer.parseInt(args.get(1));
                int i = 0;
                for (Key key : book.getGlobalKeyList()) {
                    if (i++ == target) {
                        return key.getName() + "\nRAW:\n" + raw(book, key) + "\nOSIS:\n" + osis(book, key);
                    }
                }
                return "!no key " + target;
            }
            case "corpus.chapters": {
                // Bibles and commentaries: OSIS of each whole chapter, hashed.
                Book book = book(args.get(0));
                if (book.getBookCategory() != BookCategory.BIBLE && book.getBookCategory() != BookCategory.COMMENTARY) {
                    return "";
                }
                Versification v11n = v11nOf(book);
                StringBuilder sb = new StringBuilder();
                for (BibleBook bb = v11n.getFirstBook(); bb != null; bb = v11n.getNextBook(bb)) {
                    for (int ch = 1; ch <= v11n.getLastChapter(bb); ch++) {
                        String ref = bb.getOSIS() + "." + ch;
                        String out;
                        try {
                            out = sha(osis(book, PassageKeyFactory.instance().getKey(v11n, ref)));
                        } catch (Exception e) {
                            out = fail(e);
                        }
                        sb.append(ref).append('\t').append(out).append('\n');
                    }
                }
                return sb.toString();
            }
            case "passage.parseAll": {
                Versification v11n = Versifications.instance().getVersification(args.get(0));
                StringBuilder sb = new StringBuilder();
                for (String ref : args.get(1).split("\n")) {
                    sb.append(ref).append(" => ");
                    try {
                        Passage p = PassageKeyFactory.instance().getKey(v11n, ref);
                        sb.append(p.getOsisRef()).append(" | ").append(p.getName()).append(" | ").append(p.countVerses())
                          .append(" | ").append(p.countRanges(RestrictionType.NONE)).append(" | ").append(p.countRanges(RestrictionType.CHAPTER));
                        Passage blurred = PassageKeyFactory.instance().getKey(v11n, ref);
                        blurred.blur(1, RestrictionType.CHAPTER);
                        sb.append(" | ").append(blurred.getName());
                        sb.append(" | ").append(p.hashCode()).append(" | ").append(p.equals(blurred));
                    } catch (Exception e) {
                        sb.append(fail(e));
                    }
                    sb.append('\n');
                }
                return sb.toString();
            }
            case "passage.tally": {
                Versification v11n = Versifications.instance().getVersification(args.get(0));
                PassageTally tally = new PassageTally(v11n);
                for (String ref : args.get(1).split("\n")) {
                    try {
                        tally.addAll(PassageKeyFactory.instance().getKey(v11n, ref));
                    } catch (Exception e) {
                        // invalid refs are part of the corpus; skip them here
                    }
                }
                StringBuilder sb = new StringBuilder();
                tally.setOrdering(PassageTally.Order.BIBLICAL);
                sb.append("BIBLICAL ").append(tally.getName()).append('\n');
                tally.setOrdering(PassageTally.Order.TALLY);
                sb.append("TALLY ").append(tally.getName()).append('\n');
                sb.append("TOP5 ").append(tally.getName(5)).append('\n');
                sb.append("verses ").append(tally.countVerses()).append('\n');
                return sb.toString();
            }
            case "v11n.names": {
                List<String> names = new ArrayList<>();
                for (Iterator<String> it = Versifications.instance().iterator(); it.hasNext();) {
                    names.add(it.next());
                }
                Collections.sort(names);
                return String.join("\n", names);
            }
            case "v11n.dump": {
                Versification v = Versifications.instance().getVersification(args.get(0));
                StringBuilder sb = new StringBuilder();
                sb.append("name=").append(v.getName()).append(" books=").append(v.getBookCount())
                  .append(" max=").append(v.maximumOrdinal()).append('\n');
                for (BibleBook bb = v.getFirstBook(); bb != null; bb = v.getNextBook(bb)) {
                    sb.append(bb.getOSIS()).append(" ord=").append(v.getOrdinal(bb)).append(" ch=").append(v.getLastChapter(bb)).append(" v=");
                    for (int ch = 0; ch <= v.getLastChapter(bb); ch++) {
                        sb.append(v.getLastVerse(bb, ch)).append(',');
                    }
                    sb.append('\n');
                }
                for (int o = 0; o <= v.maximumOrdinal(); o++) {
                    Verse verse = v.decodeOrdinal(o);
                    sb.append(o).append('=').append(verse.getOsisID()).append(v.getOrdinal(verse) == o ? "" : " MISMATCH").append('\n');
                }
                return sb.toString();
            }
            case "v11n.mapKjv": {
                Versification from = Versifications.instance().getVersification(args.get(0));
                Versification kjv = Versifications.instance().getVersification("KJV");
                StringBuilder sb = new StringBuilder();
                for (String ref : args.get(1).split("\n")) {
                    sb.append(ref).append(" => ");
                    try {
                        sb.append(VersificationsMapper.instance().map(PassageKeyFactory.instance().getKey(from, ref), kjv).getOsisRef());
                    } catch (Exception e) {
                        sb.append(fail(e));
                    }
                    sb.append('\n');
                }
                return sb.toString();
            }
            case "conf.parse": {
                File conf = new File(args.get(0));
                StringBuilder sb = new StringBuilder();
                try {
                    URI root = conf.getParentFile().getParentFile().toURI();
                    SwordBookMetaData md = fileMetadata(conf, root);
                    sb.append("initials=").append(md.getInitials()).append(" category=").append(md.getBookCategory()).append('\n');
                    dumpMeta(sb, md);
                } catch (Exception e) {
                    sb.append(fail(e));
                }
                return sb.toString();
            }
            case "corpus.driver.scan": {
                // args: dir (a bookDir holding mods.d). Runs the SwordBookDriver discovery scan with that dir as the
                // download dir and lists the books found under it; a header-less conf must not abort the scan.
                File dir = new File(args.get(0));
                StringBuilder sb = new StringBuilder();
                File previous = downloadDir();
                try {
                    setDownloadDir(dir);
                    org.crosswire.jsword.book.Book[] found = new org.crosswire.jsword.book.sword.SwordBookDriver().getBooks();
                    java.util.TreeSet<String> names = new java.util.TreeSet<String>();
                    for (org.crosswire.jsword.book.Book b : found) {
                        URI loc = b.getBookMetaData().getLocation();
                        if (loc != null && loc.getPath().startsWith(dir.toURI().getPath())) {
                            names.add(b.getInitials());
                        }
                    }
                    sb.append("found=").append(names).append('\n');
                } catch (Exception e) {
                    sb.append(fail(e));
                } finally {
                    setDownloadDir(previous);
                }
                return sb.toString();
            }
            case "corpus.osis.rtf": {
                // args: RTF snippets separated by U+0001. Serialized OSIS (raw format) or the exception.
                StringBuilder sb = new StringBuilder();
                for (String rtf : args.get(0).split("\u0001", -1)) {
                    sb.append(rtf).append(" => ");
                    try {
                        sb.append(rawContent(org.crosswire.jsword.book.OSISUtil.rtfToOsis(rtf)));
                    } catch (Exception e) {
                        sb.append(fail(e));
                    }
                    sb.append('\n');
                }
                return sb.toString();
            }
            case "corpus.filter": {
                // args: filter ("gbf", "osis", "thml" or "plaintext"), initials, inputs separated by U+0001. Serialized
                // OSIS (raw format) of each filter run, or the exception; line breaks of input and output are escaped.
                StringBuilder sb = new StringBuilder();
                try {
                    Book b = book(args.get(1));
                    Key key = b.getKey("Gen 1:1");
                    org.crosswire.jsword.book.filter.SourceFilter filter;
                    switch (args.get(0)) {
                        case "gbf":
                            filter = new org.crosswire.jsword.book.filter.gbf.GBFFilter();
                            break;
                        case "osis":
                            filter = new org.crosswire.jsword.book.filter.osis.OSISFilter();
                            break;
                        case "thml":
                            filter = new org.crosswire.jsword.book.filter.thml.THMLFilter();
                            break;
                        default:
                            filter = new org.crosswire.jsword.book.filter.plaintext.PlainTextFilter();
                            break;
                    }
                    for (String input : args.get(2).split("\u0001", -1)) {
                        String line;
                        try {
                            line = rawContent(filter.clone().toOSIS(b, key, input));
                        } catch (Exception e) {
                            line = fail(e);
                        }
                        sb.append(escapeBreaks(input)).append(" => ").append(escapeBreaks(line)).append('\n');
                    }
                } catch (Exception e) {
                    return fail(e);
                }
                return sb.toString();
            }
            case "corpus.osis.diff": {
                // args: pairs "source U+0002 target" separated by U+0001. diffToOsis of the cleaned-up diff.
                StringBuilder sb = new StringBuilder();
                for (String pair : args.get(0).split("\u0001", -1)) {
                    String[] st = pair.split("\u0002", -1);
                    sb.append(pair.replace('\u0002', '|')).append(" => ");
                    try {
                        List<org.crosswire.common.diff.Difference> diffs = new org.crosswire.common.diff.Diff(st[0], st[1], false).compare();
                        org.crosswire.common.diff.DiffCleanup.cleanupSemantic(diffs);
                        sb.append(rawContent(org.crosswire.jsword.book.OSISUtil.diffToOsis(diffs)));
                    } catch (Exception e) {
                        sb.append(fail(e));
                    }
                    sb.append('\n');
                }
                return sb.toString();
            }
            case "corpus.bookdata.compare": {
                // args: initials1, initials2, ref, compare ("true"/"false"). OSIS fragment of a multi-book BookData.
                try {
                    Book b1 = book(args.get(0));
                    Book b2 = book(args.get(1));
                    Key key = PassageKeyFactory.instance().getKey(v11nOf(b1), args.get(2));
                    BookData data = new BookData(new Book[] {b1, b2}, key, Boolean.parseBoolean(args.get(3)));
                    return new XMLOutputter(Format.getRawFormat()).outputString(data.getOsisFragment());
                } catch (Exception e) {
                    return fail(e);
                }
            }
            case "corpus.osis.framework": {
                // args: initials. createOsisFramework of the book's metadata, and the whole BookData.getOsis().
                try {
                    Book b = book(args.get(0));
                    XMLOutputter out = new XMLOutputter(Format.getRawFormat());
                    Key key = PassageKeyFactory.instance().getKey(v11nOf(b), "Gen 1:1");
                    return out.outputString(org.crosswire.jsword.book.OSISUtil.createOsisFramework(b.getBookMetaData()))
                        + "\n" + out.outputString(new BookData(b, key).getOsis());
                } catch (Exception e) {
                    return fail(e);
                }
            }
            case "corpus.osis.verse": {
                // args: v11n name. getVerse from several hand-built elements.
                Versification v11n = Versifications.instance().getVersification(args.get(0));
                org.crosswire.jsword.book.OSISUtil.OSISFactory f = org.crosswire.jsword.book.OSISUtil.factory();
                StringBuilder sb = new StringBuilder();
                String[] ids = {"Gen.1.1", "Rev.22.21", "Gen.1.2", "Foo.1.1", "", "Gen.99.99"};
                for (String id : ids) {
                    // verse itself, a hi inside the verse, and a verse-less tree
                    Element verse = f.createVerse();
                    verse.setAttribute("osisID", id);
                    Element hi = f.createHI();
                    verse.addContent(hi);
                    Element lone = f.createDiv();
                    Element[] probes = {verse, hi, lone};
                    for (Element e : probes) {
                        sb.append(id).append(' ').append(e.getName()).append(" => ");
                        try {
                            Verse v = org.crosswire.jsword.book.OSISUtil.getVerse(v11n, e);
                            sb.append(v == null ? "<null>" : v.getOsisID());
                        } catch (Exception ex) {
                            sb.append(fail(ex));
                        }
                        sb.append('\n');
                    }
                }
                return sb.toString();
            }
            case "corpus.defaultbook": {
                StringBuilder sb = new StringBuilder();
                org.crosswire.jsword.book.DefaultBook d = new org.crosswire.jsword.book.DefaultBook(Books.installed(), org.crosswire.jsword.book.BookFilters.getOnlyBibles());
                sb.append(d.getDefault()).append(' ').append(d.getDefaultName()).append('\n');
                d.setDefaultByName("");
                d.setDefaultByName("NoSuchBook");
                sb.append(d.getDefault()).append('\n');
                d.setDefaultByName("KJV");
                sb.append(d.getDefaultName()).append('\n');
                d.setDefaultConditionally(book("ESV2011"));
                sb.append(d.getDefaultName()).append('\n');
                d.setDefault(book("ESV2011"));
                sb.append(d.getDefaultName()).append('\n');
                // not a Bible: rejected by the filter
                d.setDefault(book("Nave"));
                sb.append(d.getDefaultName()).append('\n');
                org.crosswire.jsword.book.DefaultBook fresh = new org.crosswire.jsword.book.DefaultBook(Books.installed(), org.crosswire.jsword.book.BookFilters.getOnlyBibles());
                fresh.setDefaultConditionally(book("Nave"));
                sb.append(fresh.getDefault()).append('\n');
                fresh.setDefaultConditionally(book("KJV"));
                sb.append(fresh.getDefaultName()).append('\n');
                return sb.toString();
            }
            default:
                throw new IllegalArgumentException("Unknown parity op: " + op);
        }
    }

    private static Book book(String initials) {
        Book b = Books.installed().getBook(initials);
        if (b == null) {
            throw new IllegalArgumentException("no installed book " + initials);
        }
        return b;
    }

    /**
     * The baseline's getPropertyKeys() returns null for Sword metadata, so every KEY_* constant of
     * SwordBookMetaData and BookMetaData is probed through getProperty/getValues, plus the typed getters.
     */
    private static void dumpMeta(StringBuilder sb, BookMetaData md) throws Exception {
        List<String> keys = new ArrayList<>();
        for (Class<?> c : new Class<?>[] {SwordBookMetaData.class, BookMetaData.class}) {
            for (java.lang.reflect.Field f : c.getFields()) {
                if (f.getName().startsWith("KEY_") && f.getType() == String.class
                        && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    String k = (String) f.get(null);
                    if (!keys.contains(k)) {
                        keys.add(k);
                    }
                }
            }
        }
        Collections.sort(keys);
        for (String k : keys) {
            sb.append(k).append('=').append(md.getProperty(k)).append(" | ").append(md.getValues(k)).append('\n');
        }
        sb.append("typed ").append(md.getInitials()).append(' ').append(md.getAbbreviation()).append(' ').append(md.getOsisID())
          .append(' ').append(md.isSupported()).append(' ').append(md.isEnciphered()).append(' ').append(md.isLocked())
          .append(' ').append(md.isLeftToRight()).append(' ').append(md.getBookCharset()).append(' ').append(md.getKeyType())
          .append(' ').append(md.getDriverName()).append(' ').append(md.isQuestionable()).append('\n');
    }

    private static Versification v11nOf(Book book) {
        String name = book.getBookMetaData().getProperty("Versification");
        return Versifications.instance().getVersification(name == null ? "KJV" : name);
    }

    private static String safeOsisRef(Key key) {
        try {
            return key.getOsisRef();
        } catch (Exception e) {
            return fail(e);
        }
    }

    private static String raw(Book book, Key key) {
        try {
            return String.valueOf(book.getRawText(key));
        } catch (Exception e) {
            return fail(e);
        }
    }

    private static String osis(Book book, Key key) {
        try {
            Element frag = new BookData(book, key).getOsisFragment();
            return new XMLOutputter(Format.getRawFormat()).outputString(frag);
        } catch (Exception e) {
            return fail(e);
        }
    }

    private static String rawContent(List<org.jdom2.Content> content) {
        XMLOutputter out = new XMLOutputter(Format.getRawFormat());
        return out.outputString(content);
    }

    private static String escapeBreaks(String s) {
        return s.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String fail(Exception e) {
        // The class loader's identity is the only legitimate difference between the two sides.
        String msg = String.valueOf(e.getMessage()).replaceAll("loader [^)]*\\)", "loader X)");
        return "!" + e.getClass().getName() + ": " + msg;
    }

    private static String sha(String s) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

}
