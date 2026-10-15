package org.crosswire.jsword.book.filter

import java.lang.reflect.Proxy
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.versification.system.Versifications

// Only the production members consumed by filters are supplied. Unexpected facade calls fail loudly.
internal actual fun filterTestBook(initials: String): Book {
    var metadata = SwordBookMetaData("[$initials]\nDescription=Frozen filter context\nModDrv=RawText\nEncoding=UTF-8\n".encodeToByteArray(), initials)
    return Proxy.newProxyInstance(Book::class.java.classLoader, arrayOf(Book::class.java)) { _, method, args ->
        when (method.name) {
            "getInitials" -> initials
            "getBookMetaData" -> metadata
            "setBookMetaData" -> { metadata = args!![0] as SwordBookMetaData; null }
            "getKey" -> PassageKeyFactory.instance().getKey(Versifications.instance().getVersification("KJV"), args!![0] as String?)
            "toString" -> initials
            else -> error("Unexpected filter facade call: ${method.name}")
        }
    } as Book
}
