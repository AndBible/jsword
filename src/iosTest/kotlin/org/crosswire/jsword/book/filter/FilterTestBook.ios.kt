package org.crosswire.jsword.book.filter

import fi.sykero.kdom.Content
import fi.sykero.kdom.Document
import org.crosswire.common.activate.Lock
import org.crosswire.common.util.Language
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.IndexStatusListener
import org.crosswire.jsword.index.search.SearchRequest
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.versification.system.Versifications

internal actual fun filterTestBook(initials: String): Book = object : Book {
    override val initials: String = initials
    override var bookMetaData: BookMetaData = SwordBookMetaData(
        "[$initials]\nDescription=Frozen filter context\nModDrv=RawText\nEncoding=UTF-8\n".encodeToByteArray(), initials)
    override fun getKey(name: String?): Key = PassageKeyFactory.instance().getKey(
        Versifications.instance().getVersification("KJV"), name)
    override fun addIndexStatusListener(li: IndexStatusListener?) = Unit
    override fun removeIndexStatusListener(li: IndexStatusListener?) = Unit
    override fun activate(lock: Lock?) = Unit
    override fun deactivate(lock: Lock?) = Unit
    override fun compareTo(other: Book): Int = initials.compareTo(other.initials)
    // Only the members consumed by filters are supplied; every other facade call fails loudly (like the JVM proxy).
    private fun unexpected(member: String): Nothing = error("Unexpected filter facade call: $member")
    override fun getGlobalKeyList(): Key = unexpected("getGlobalKeyList")
    override fun getValidKey(name: String?): Key = unexpected("getValidKey")
    override fun createEmptyKeyList(): Key = unexpected("createEmptyKeyList")
    override fun getOsisIterator(key: Key?, allowEmpty: Boolean, allowGenTitles: Boolean): MutableIterator<Content> = unexpected("getOsisIterator")
    override fun contains(key: Key?): Boolean = unexpected("contains")
    override fun getRawText(key: Key?): String = unexpected("getRawText")
    override fun isWritable(): Boolean = unexpected("isWritable")
    override fun setRawText(key: Key?, rawData: String?): Unit = unexpected("setRawText")
    override fun setAliasKey(alias: Key?, source: Key?): Unit = unexpected("setAliasKey")
    override fun find(request: SearchRequest?): Key? = unexpected("find")
    override fun find(request: String?): Key? = unexpected("find")
    override fun match(name: String?): Boolean = unexpected("match")
    override fun unlock(unlockKey: String?): Boolean = unexpected("unlock")
    override fun hasFeature(feature: FeatureType?): Boolean = unexpected("hasFeature")
    override fun getProperty(key: String?): String? = unexpected("getProperty")
    override fun putProperty(key: String?, value: String?): Unit = unexpected("putProperty")
    override fun putProperty(key: String?, value: String?, forFrontend: Boolean): Unit = unexpected("putProperty")
    override fun toOSIS(): Document = unexpected("toOSIS")
    override fun resetSearcher(): Unit = unexpected("resetSearcher")
    override val scope: Key? get() = unexpected("scope")
    override val name: String get() = unexpected("name")
    override val bookCategory: BookCategory get() = unexpected("bookCategory")
    override val driver: BookDriver? get() = unexpected("driver")
    override val language: Language get() = unexpected("language")
    override val abbreviation: String get() = unexpected("abbreviation")
    override val osisID: String get() = unexpected("osisID")
    override val isSupported: Boolean get() = unexpected("isSupported")
    override val isEnciphered: Boolean get() = unexpected("isEnciphered")
    override val isLocked: Boolean get() = unexpected("isLocked")
    override val unlockKey: String? get() = unexpected("unlockKey")
    override val isQuestionable: Boolean get() = unexpected("isQuestionable")
    override val driverName: String? get() = unexpected("driverName")
    override val isLeftToRight: Boolean get() = unexpected("isLeftToRight")
    override val propertyKeys: Set<String>? get() = unexpected("propertyKeys")
    override var indexStatus: IndexStatus
        get() = unexpected("indexStatus")
        set(_) = unexpected("indexStatus")
}
