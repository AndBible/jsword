package org.crosswire.jsword.book.install

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.crosswire.jsword.book.install.sword.AbstractSwordInstaller
import org.crosswire.jsword.book.install.sword.HttpSwordInstallerFactory
import org.crosswire.jsword.book.install.sword.HttpsSwordInstallerFactory

/**
 * The installer definition parsing of the common HTTP and HTTPS factories (a `Regex` split with limit 6 since K5b),
 * pinned against the former `Pattern.split` behaviour: empty trailing parts are kept and the sixth part keeps commas.
 * Moved from the JVM-only `InstallerCommonReadyTest` once the factories became common (rule 21).
 */
class InstallerFactoryParsingTest {
    @Test fun factoryParsesTheSixPartAndTheOldFourPartDefinition() {
        for ((factory, type) in listOf(HttpSwordInstallerFactory() to "sword-http", HttpsSwordInstallerFactory() to "sword-https")) {
            // Existing behaviour, kept: the parts after the catalog directory are read as proxy host and port
            // (the index directory slot of the definition is not skipped).
            val six = factory.createInstaller("h.invalid,/pkg,/cat,proxy.invalid,8080,ignored") as AbstractSwordInstaller
            assertEquals(type, six.getType())
            assertEquals("h.invalid,/pkg,/cat,,proxy.invalid,8080", six.getInstallerDefinition())
            assertEquals("proxy.invalid", six.getProxyHost())
            assertEquals(8080, six.getProxyPort())

            // empty trailing parts are kept (limit 6), so a missing proxy gives no proxy host
            val noProxy = factory.createInstaller("h.invalid,/pkg,/cat,,,") as AbstractSwordInstaller
            assertNull(noProxy.getProxyHost())
            assertNull(noProxy.getProxyPort())

            // the old format: host,dir,proxyHost,proxyPort with the fixed sub directories
            val old = factory.createInstaller("h.invalid,/dir,p.invalid,81") as AbstractSwordInstaller
            assertEquals("/dir/packages/rawzip", old.getPackageDirectory())
            assertEquals("/dir/raw", old.getCatalogDirectory())
            assertEquals("p.invalid", old.getProxyHost())
            assertEquals(81, old.getProxyPort())
        }
    }

    @Test fun factoryRejectsTheWrongNumberOfParts() {
        for (factory in listOf(HttpSwordInstallerFactory(), HttpsSwordInstallerFactory())) {
            for (bad in listOf("", "a", "a,b,c", "a,b,c,d,e")) {
                val e = assertFailsWith<IllegalArgumentException>(bad) { factory.createInstaller(bad) }
                assertEquals("Not enough / symbols in url: $bad", e.message)
            }
            // a six part definition whose port is not a number
            assertFailsWith<NumberFormatException> { factory.createInstaller("h,/p,/c,px,notaport,x") }
            // the sixth part keeps further commas, so a seven part definition is read as six parts
            assertEquals("h,/p,/c,,,", (factory.createInstaller("h,/p,/c,,,x,y") as AbstractSwordInstaller).getInstallerDefinition())
        }
    }
}
