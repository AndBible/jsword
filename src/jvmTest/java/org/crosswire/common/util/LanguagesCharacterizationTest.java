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
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.util;

import java.util.Locale;

import org.crosswire.jsword.internationalisation.DefaultLocaleProvider;
import org.crosswire.jsword.internationalisation.LocaleProvider;
import org.crosswire.jsword.internationalisation.LocaleProviderManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Characterization of {@link Languages} and {@link Language}, run with the English locale
 * (iso639_en.properties over iso639.properties) so the names are deterministic.
 * Expected values were derived by reading the properties files and the lookup code.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class LanguagesCharacterizationTest {
    private static final String GRC_NAME = "Greek, Ancient [to 1453] (Ἑλληνική)";
    private static final String HE_NAME = "Hebrew (עברית)";
    private static final String ZH_HANT_NAME = "Chinese, Traditional (繁体中文)";

    @Before
    public void useEnglish() {
        LocaleProviderManager.setLocaleProvider(new LocaleProvider() {
            public Locale getUserLocale() {
                return Locale.ENGLISH;
            }
        });
    }

    @After
    public void restoreLocale() {
        LocaleProviderManager.setLocaleProvider(new DefaultLocaleProvider());
    }

    @Test
    public void testLanguagesGetName() {
        Assert.assertEquals("English", Languages.getName("en"));
        Assert.assertEquals("Finnish (Suomi)", Languages.getName("fi"));
        Assert.assertEquals(GRC_NAME, Languages.getName("grc"));
        Assert.assertEquals(HE_NAME, Languages.getName("he"));
        // A missing key falls back to the code itself
        Assert.assertEquals("x-unknown", Languages.getName("x-unknown"));
        Assert.assertEquals("", Languages.getName(""));
    }

    @Test
    public void testLanguagesGetNameNull() {
        try {
            Languages.getName(null);
            Assert.fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            // ResourceBundle.getString(null) throws NPE; only MissingResourceException is caught
        }
    }

    @Test
    public void testIsRtoL() {
        Assert.assertTrue(Languages.RtoL.isRtoL(null, "he"));
        // A script wins over the language, even when it says the opposite
        Assert.assertFalse(Languages.RtoL.isRtoL("Latn", "he"));
        Assert.assertTrue(Languages.RtoL.isRtoL("Arab", "en"));
        Assert.assertFalse(Languages.RtoL.isRtoL(null, null));
    }

    @Test
    public void testLanguageEnUs() {
        Language l = new Language("en-US");
        Assert.assertEquals("en", l.getCode());
        Assert.assertNull(l.getScript());
        Assert.assertEquals("US", l.getCountry());
        Assert.assertEquals("English (US)", l.getName());
        Assert.assertEquals("English (US)", l.toString());
        Assert.assertTrue(l.isLeftToRight());
        Assert.assertTrue(l.isValidLanguage());
        Assert.assertEquals("en-US", l.getGivenSpecification());
        Assert.assertEquals("en-US", l.getFoundSpecification());
    }

    @Test
    public void testLanguageZhHantTw() {
        Language l = new Language("zh-Hant-TW");
        Assert.assertEquals("zh", l.getCode());
        Assert.assertEquals("Hant", l.getScript());
        Assert.assertEquals("TW", l.getCountry());
        // zh-Hant-TW has no entry; the lookup falls back to zh-Hant
        Assert.assertEquals(ZH_HANT_NAME, l.getName());
        Assert.assertEquals("zh-Hant", l.getFoundSpecification());
        Assert.assertTrue(l.isLeftToRight());
        Assert.assertTrue(l.isValidLanguage());
    }

    @Test
    public void testLanguageFi() {
        Language l = new Language("fi");
        Assert.assertEquals("fi", l.getCode());
        Assert.assertNull(l.getScript());
        Assert.assertNull(l.getCountry());
        Assert.assertEquals("Finnish (Suomi)", l.getName());
        Assert.assertTrue(l.isLeftToRight());
    }

    @Test
    public void testLanguageHebrewIsRightToLeft() {
        Language l = new Language("he");
        Assert.assertEquals(HE_NAME, l.getName());
        Assert.assertFalse(l.isLeftToRight());
        Assert.assertFalse("a second call uses the cached answer", l.isLeftToRight());
    }

    @Test
    public void testLanguageUnd() {
        Language l = new Language("und");
        Assert.assertEquals("und", l.getCode());
        Assert.assertEquals("Undetermined", l.getName());
        Assert.assertTrue(l.isValidLanguage());
    }

    @Test
    public void testLanguageEmptyIsUndeterminedAndInvalid() {
        Language l = new Language("");
        // Fewer than 2 characters: code is "und" and the language is invalid
        Assert.assertEquals("und", l.getCode());
        Assert.assertNull(l.getScript());
        Assert.assertNull(l.getCountry());
        Assert.assertEquals("Undetermined", l.getName());
        Assert.assertFalse(l.isValidLanguage());
        Assert.assertEquals("", l.getGivenSpecification());
        Assert.assertEquals("und", l.getFoundSpecification());
        Assert.assertTrue(l.isLeftToRight());
    }

    @Test
    public void testLanguageNullIsEnglish() {
        Language l = new Language(null);
        Assert.assertEquals("en", l.getCode());
        Assert.assertEquals("English", l.getName());
        Assert.assertNull(l.getGivenSpecification());
        Assert.assertTrue(l.isValidLanguage());
        Assert.assertEquals(Language.DEFAULT_LANG, l);
        Assert.assertEquals("en", Language.DEFAULT_LANG.getCode());
    }

    @Test
    public void testLanguageUnknownCode() {
        Language l = new Language("zzz");
        Assert.assertEquals("zzz", l.getCode());
        Assert.assertEquals("zzz", l.getName());
        Assert.assertFalse(l.isValidLanguage());
        Assert.assertEquals("zzz", l.getFoundSpecification());
        // A leading "x-" makes the second character a '-'
        Language x = new Language("x-foo");
        Assert.assertEquals("und", x.getCode());
        Assert.assertFalse(x.isValidLanguage());
    }

    @Test
    public void testLanguageCanonicalisationAndInvalidShapes() {
        Language l = new Language("EN-latn-us");
        Assert.assertEquals("en", l.getCode());
        Assert.assertEquals("Latn", l.getScript());
        Assert.assertEquals("US", l.getCountry());
        // A fourth part makes the specification invalid, though code/script/country are kept
        Language extra = new Language("en-US-x-foo");
        Assert.assertEquals("en", extra.getCode());
        Assert.assertEquals("US", extra.getCountry());
        Assert.assertFalse(extra.isValidLanguage());
        // A part of the wrong length
        Assert.assertFalse(new Language("en-USA").isValidLanguage());
    }

    @Test
    public void testEqualsHashCodeCompareTo() {
        Language a = new Language("en-US");
        Language b = new Language("EN-us");
        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(0, a.compareTo(b));
        Assert.assertTrue(a.equals(a));
        Assert.assertFalse(a.equals(null));
        Assert.assertFalse(a.equals("en-US"));

        Language en = new Language("en");
        Language fi = new Language("fi");
        Assert.assertFalse(en.equals(a));
        Assert.assertFalse(en.equals(fi));
        // compareTo compares the names: "English" < "Finnish (Suomi)"
        Assert.assertTrue(en.compareTo(fi) < 0);
        Assert.assertTrue(fi.compareTo(en) > 0);

        // "" and "und" are the same language
        Assert.assertEquals(new Language(""), new Language("und"));
        Assert.assertEquals(new Language("").hashCode(), new Language("und").hashCode());

        // Not equal (country differs) but same name, same found spec: hashes and ordering agree anyway
        Language hant = new Language("zh-Hant");
        Language hantTw = new Language("zh-Hant-TW");
        Assert.assertFalse(hant.equals(hantTw));
        Assert.assertEquals(hant.hashCode(), hantTw.hashCode());
        Assert.assertEquals(0, hant.compareTo(hantTw));
    }
}
