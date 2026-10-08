package org.crosswire.ksword.parity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    static String apply(String op, List<String> args) throws Exception {
        if (op.equals("passage.parseAllFi")) {
            // Same as passage.parseAll, but with the Finnish locale so BibleNames_fi names resolve.
            final java.util.Locale fi = new java.util.Locale("fi", "FI");
            org.crosswire.jsword.internationalisation.LocaleProvider old =
                org.crosswire.jsword.internationalisation.LocaleProviderManager.getLocaleProvider();
            org.crosswire.jsword.internationalisation.LocaleProviderManager.setLocaleProvider(
                new org.crosswire.jsword.internationalisation.LocaleProvider() {
                    public java.util.Locale getUserLocale() {
                        return fi;
                    }
                });
            try {
                return apply("passage.parseAll", args);
            } finally {
                org.crosswire.jsword.internationalisation.LocaleProviderManager.setLocaleProvider(old);
            }
        }
        switch (op) {
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
                    SwordBookMetaData md = new SwordBookMetaData(conf, root);
                    sb.append("initials=").append(md.getInitials()).append(" category=").append(md.getBookCategory()).append('\n');
                    dumpMeta(sb, md);
                } catch (Exception e) {
                    sb.append(fail(e));
                }
                return sb.toString();
            }
            case "ser.write": {
                // args: kind (Rocket|Bitwise|Distinct|Ranged|Tally|Verse|VerseRange), v11n, ref
                Versification v11n = Versifications.instance().getVersification(args.get(1));
                Passage parsed = PassageKeyFactory.instance().getKey(v11n, args.get(2));
                Object obj;
                switch (args.get(0)) {
                    case "Rocket": { Passage p = new RocketPassage(v11n); p.addAll(parsed); obj = p; break; }
                    case "Bitwise": { Passage p = new BitwisePassage(v11n); p.addAll(parsed); obj = p; break; }
                    case "Distinct": { Passage p = new DistinctPassage(v11n); p.addAll(parsed); obj = p; break; }
                    case "Ranged": { Passage p = new RangedPassage(v11n); p.addAll(parsed); obj = p; break; }
                    case "Tally": { PassageTally p = new PassageTally(v11n); p.addAll(parsed); obj = p; break; }
                    case "Verse": obj = parsed.getVerseAt(0); break;
                    case "VerseRange": obj = parsed.getRangeAt(0, RestrictionType.NONE); break;
                    default: throw new IllegalArgumentException(args.get(0));
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(obj);
                }
                return hex(bos.toByteArray());
            }
            case "ser.read": {
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(unhex(args.get(0))))) {
                    Object o = ois.readObject();
                    String ref = o instanceof Key ? ((Key) o).getOsisRef() : String.valueOf(o);
                    String extra = o instanceof Verse ? " id=" + ((Verse) o).getOsisID()
                        : o instanceof VerseRange ? " card=" + ((VerseRange) o).getCardinality()
                        : o instanceof Passage ? " verses=" + ((Passage) o).countVerses() : "";
                    return o.getClass().getName() + " " + ref + extra;
                } catch (Exception e) {
                    return fail(e);
                }
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

    private static byte[] unhex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
