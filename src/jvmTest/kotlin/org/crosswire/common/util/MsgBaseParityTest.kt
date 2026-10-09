package org.crosswire.common.util

import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.internationalisation.LocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

class MsgBaseParityTest {
    private lateinit var savedProvider: LocaleProvider
    private lateinit var savedDefault: Locale
    @Before fun save() { savedProvider = LocaleProviderManager.getLocaleProvider(); savedDefault = Locale.getDefault() }
    @After fun restore() { LocaleProviderManager.setLocaleProvider(savedProvider); Locale.setDefault(savedDefault) }

    /** The old MsgBase.lookup, written out with the JDK: bundle by provider locale, MessageFormat by default locale. */
    private fun oldLookup(base: String, key: String, params: Array<Any?>): String {
        val l = LocaleProviderManager.getLocale()
        val raw = try { ResourceBundle.getBundle(base, l, CWClassLoader.instance(MsgBaseParityTest::class.java)).getString(key) } catch (e: Exception) { key }
        val text = if (params.isEmpty()) raw else MessageFormat.format(raw.replace("'", "''"), *params)
        return org.crosswire.common.icu.NumberShaper().shape(text)!!
    }

    /**
     * NumberShaper caches the script's "nine" per instance (a pre-existing quirk), and the shared Msg singletons
     * keep whatever the first locale in the JVM chose. Reset it so each locale is compared from a clean slate.
     */
    private fun resetShapers() {
        for (cls in listOf(JSMsg::class.java, JSOtherMsg::class.java)) {
            val msgField = cls.getDeclaredField("msg").apply { isAccessible = true }
            val shaperField = MsgBase::class.java.getDeclaredField("shaper").apply { isAccessible = true }
            val shaper = shaperField.get(msgField.get(null))
            val nine = org.crosswire.common.icu.NumberShaper::class.java.getDeclaredField("nineShape").apply { isAccessible = true }
            nine.setChar(shaper, '\u0000')
        }
    }

    @Test fun everyParameterizedMessageInSeveralLocales() {
        val keys = PropertiesParser.parse(KswordResources.bytes("JSMsg.properties")!!, PropertiesCharset.UTF8_THEN_LATIN1).keys
        val params = listOf<Array<Any?>>(arrayOf(), arrayOf(0), arrayOf(1), arrayOf(2), arrayOf(1234, 5678L), arrayOf("x", 1, 2, 1000000),
            arrayOf(null, -1), arrayOf(2.5, "s"))
        for (tag in listOf("en", "fi", "de", "fa", "zh_CN", "ar")) {
            val l = Locale(tag.substringBefore('_'), tag.substringAfter('_', ""))
            LocaleProviderManager.setLocaleProvider(object : LocaleProvider { override fun getUserLocale() = l })
            Locale.setDefault(l)
            resetShapers()
            for (key in keys) for (p in params) {
                val expected = runCatching { oldLookup("JSMsg", key, p) }
                val actual = runCatching { JSMsg.gettext(key, *p) }
                assertEquals("$tag/$key/${p.toList()}", expected.getOrNull(), actual.getOrNull())
                assertEquals("$tag/$key/${p.toList()} throws", expected.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass)
            }
        }
        for (tag in listOf("en", "de", "fa")) {
            val l = Locale(tag)
            LocaleProviderManager.setLocaleProvider(object : LocaleProvider { override fun getUserLocale() = l })
            Locale.setDefault(l)
            resetShapers()
            assertEquals(oldLookup("JSOtherMsg", "Book must not be null", arrayOf()), JSOtherMsg.lookupText("Book must not be null"))
            assertEquals(oldLookup("JSOtherMsg", "Cannot find resource: {0}", arrayOf("x.properties")), JSOtherMsg.lookupText("Cannot find resource: {0}", "x.properties"))
        }
    }
}
