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
 * © CrossWire Bible Society, 2012 - 2016
 *
 */
package org.crosswire.jsword.versification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
public class BibleNamesTest {
    private boolean fullName;

    /**
     * {@link BookName#setFullBookName(boolean)} is global state that several other test classes
     * change without restoring it. Pin it here so that these tests do not depend on test order.
     */
    @Before
    public void setUp() {
        fullName = BookName.isFullBookName();
        BookName.setFullBookName(true);
    }

    @After
    public void tearDown() {
        BookName.setFullBookName(fullName);
    }

    @Test
    public void testLoadEnglish() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
    }

    @Test
    public void testLoadAF() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("af")));
    }

    @Test
    public void testLoadEgyptianArabic() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("ar", "EG")));
    }

    @Test
    public void testLoadBG() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("bg")));
    }

    @Test
    public void testLoadCS() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("cs")));
    }

    @Test
    public void testLoadCY() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("cy")));
    }

    @Test
    public void testLoadDanish() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("da")));
    }

    @Test
    public void testLoadGerman() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("de")));
    }

    @Test
    public void testLoadSpanish() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("es")));
    }

    @Test
    public void testLoadET() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("et")));
    }

    @Test
    public void testLoadFarsi() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("fa")));
    }

    @Test
    public void testLoadFinnish() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("fi")));
    }

    @Test
    public void testLoadFO() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("fo")));
    }

    @Test
    public void testLoadFrench() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("fr")));
    }

    @Test
    public void testLoadHebrew() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("he")));
    }

    @Test
    public void testLoadHU() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("hu")));
    }

    @Test
    public void testLoadID() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("id")));
    }

    @Test
    public void testLoadIN() {
        // "in" is the legacy ISO code for Indonesian (modern "id"); both must yield
        // Indonesian book names, never Hindi. BibleNames_in.properties previously held
        // a copy of the Hindi names, which surfaced as Hindi for Indonesian users on
        // runtimes that resolve Indonesian to the "in" bundle (e.g. Android's ART).
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("in")));
        String viaIn = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("in")));
        String viaId = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("id")));
        String hindi = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("hi")));
        assertEquals(viaId, viaIn);
        assertNotEquals(hindi, viaIn);
    }

    @Test
    public void testLoadItalian() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("it")));
    }

    @Test
    public void testLoadIW() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("iw")));
    }

    @Test
    public void testLoadKO() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("ko")));
    }

    @Test
    public void testLoadLA() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("la")));
    }

    @Test
    public void testLoadLT() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("lt")));
    }

    @Test
    public void testLoadNB() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("nb")));
    }

    @Test
    public void testLoadDutch() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("nl")));
    }

    @Test
    public void testLoadNN() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("nn")));
    }

    @Test
    public void testLoadPL() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("pl")));
    }

    @Test
    public void testLoadBrazillianPortuguese() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("pt", "BR")));
    }

    @Test
    public void testLoadPortuguese() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("pt")));
    }

    @Test
    public void testLoadRo() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("ro")));
    }

    @Test
    public void testLoadRU() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("ru")));
    }

    @Test
    public void testLoadSK() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("sk")));
    }

    @Test
    public void testLoadSL() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("sl")));
    }

    @Test
    public void testLoadSwedish() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("sv")));
    }

    @Test
    public void testLoadThai() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("th")));
    }

    @Test
    public void testLoadTR() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("tr")));
    }

    @Test
    public void testLoadUkranian() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("uk")));
    }

    @Test
    public void testLoadVietnamese() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("vi")));
    }

    @Test
    public void testLoadChineseTraditional() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("zh", "CN")));
    }

    @Test
    public void testLoadChineseSimplified() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("zh")));
    }

    @Test
    public void testLoadSwahili() {
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(new Locale("sw")));
    }

    @Test
    public void testLoadMS() {
        Locale locale = new Locale("ms");
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String localized = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String english = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
        assertNotEquals(english, localized);
    }

    @Test
    public void testLoadCA() {
        Locale locale = new Locale("ca");
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String localized = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String english = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
        assertNotEquals(english, localized);
    }

    @Test
    public void testLoadFIL() {
        // Filipino Genesis is also "Genesis"; assert on Exodus ("Exodo") instead.
        Locale locale = new Locale("fil");
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String localized = BibleNames.instance().getPreferredNameInLocale(BibleBook.EXOD, org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String english = BibleNames.instance().getPreferredNameInLocale(BibleBook.EXOD, org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
        assertNotEquals(english, localized);
    }

    @Test
    public void testLoadNE() {
        Locale locale = new Locale("ne");
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String localized = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String english = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
        assertNotEquals(english, localized);
    }

    @Test
    public void testLoadUR() {
        Locale locale = new Locale("ur");
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String localized = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String english = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
        assertNotEquals(english, localized);
    }

    @Test
    public void testLoadUZ() {
        Locale locale = new Locale("uz");
        BibleNames.instance().load(org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String localized = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(locale));
        String english = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, org.crosswire.common.util.KLocale_jvmKt.toKLocale(Locale.ENGLISH));
        assertNotEquals(english, localized);
    }

    /**
     * Short book names are used whenever {@link BookName#isFullBookName()} is false. A locale
     * bundle that defines &lt;book&gt;.Full but omits &lt;book&gt;.Short does not fail loudly:
     * ResourceBundle silently inherits the ENGLISH abbreviation from BibleNames.properties through
     * the parent chain, so that locale ends up showing English abbreviations. Require every bundle
     * to define its own Short (and Alt) key for each Full key it defines. An empty value is fine —
     * it makes the localized full name serve as the short name.
     */
    @Test
    public void testEveryBundleDefinesItsOwnShortAndAltNames() throws IOException {
        List<String> problems = new ArrayList<String>();
        for (File bundle : listBibleNamesBundles()) {
            Properties props = new Properties();
            InputStream in = new FileInputStream(bundle);
            try {
                // Keys are ASCII, so the ISO-8859-1 decoding used by Properties is harmless here.
                props.load(in);
            } finally {
                in.close();
            }
            for (String key : props.stringPropertyNames()) {
                if (!key.endsWith(".Full")) {
                    continue;
                }
                String book = key.substring(0, key.length() - ".Full".length());
                for (String suffix : new String[] {".Short", ".Alt"}) {
                    if (!props.containsKey(book + suffix)) {
                        problems.add(bundle.getName() + " defines " + key + " but not " + book + suffix);
                    }
                }
            }
        }
        if (!problems.isEmpty()) {
            fail("Incomplete BibleNames bundles (missing keys inherit English values):\n"
                    + String.join("\n", problems));
        }
    }

    private List<File> listBibleNamesBundles() {
        URL base = BibleNames.class.getClassLoader().getResource("BibleNames.properties");
        assertNotNull("BibleNames.properties must be on the classpath", base);
        assertEquals("BibleNames.properties must be an unpacked file", "file", base.getProtocol());

        File dir;
        try {
            dir = new File(base.toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        File[] files = dir.listFiles((d, name) -> name.startsWith("BibleNames") && name.endsWith(".properties"));
        assertNotNull("Could not list " + dir, files);

        List<File> bundles = new ArrayList<File>();
        for (File file : files) {
            bundles.add(file);
        }
        assertTrue("Expected to find several BibleNames bundles, found " + bundles.size(), bundles.size() > 10);
        return bundles;
    }

}
