package org.crosswire.ksword.k5b

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import java.io.File

/** Builds a Sword book over one of the K4 fixture modules, without registering it anywhere. */
object K5bFixtureBook {
    fun create(name: String = "K4RawText"): Book {
        val root = File(checkNotNull(System.getProperty("ksword.fixturesDir")), "space dir/ä")
        val md = SwordBookMetaDataJvm.create(File(root, "mods.d/$name.conf"), root.toURI())
        md.setDriver(SwordBookDriver.instance())
        return md.bookType!!.createBook(md)
    }
}
