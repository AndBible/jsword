package org.crosswire.jsword.book.filter

import org.crosswire.common.activate.Lock
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.versification.system.Versifications

internal actual fun filterTestBook(initials: String): Book = object : Book {
    override val initials: String = initials
    override var bookMetaData: BookMetaData = SwordBookMetaData(
        "[$initials]\nDescription=Frozen filter context\nModDrv=RawText\nEncoding=UTF-8\n".encodeToByteArray(), initials)
    override fun getKey(name: String?): Key = PassageKeyFactory.instance().getKey(
        Versifications.instance().getVersification("KJV"), name)
    override fun activate(lock: Lock?) = Unit
    override fun deactivate(lock: Lock?) = Unit
    override fun compareTo(other: Book): Int = initials.compareTo(other.initials)
}
