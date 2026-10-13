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
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.sword

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

import okio.Path
import okio.IOException
import org.crosswire.common.util.KUri

import org.crosswire.common.util.Filter
import org.crosswire.common.util.IOUtil
import org.crosswire.common.util.IniSection
import org.crosswire.common.util.Language
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.PropertyMap
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.KeyType
import org.crosswire.jsword.book.MetaDataLocator
import org.crosswire.jsword.book.basic.AbstractBookMetaData
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.versification.system.Versifications
import org.crosswire.common.kdom.Document
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * A utility class for loading and representing Sword book configs.
 *
 * <p>
 * Config file format. See also: <a href=
 * "http://sword.sourceforge.net/cgi-bin/twiki/view/Swordapi/ConfFileLayout">
 * http://sword.sourceforge.net/cgi-bin/twiki/view/Swordapi/ConfFileLayout</a>
 * <p>
 * In addition, the SwordBookMetaData is hierarchical. The Level
 * indicates where the file originates from. The full hierarchy could be laid
 * out as followed:
 *
 * <pre>
 *     - sword
 *         - jsword
 *            - front-end
 * </pre>
 *
 * Various rules govern where attributes are read from. The general rule is that
 * the highest level (front-end write) will override values from the lowest
 * common denominator (sword). Various parts of the tree may be missing as the
 * files may not exist on disk. There are exceptions however and each method in
 * this file documents its behavior.
 *
 * <p>
 * The contents of the About field are in RTF.
 * <p>
 * \ is used as a continuation line.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Mark Goodwin
 * @author Joe Walker
 * @author Jacky Cheung
 * @author DM Smith
 */
class SwordBookMetaData : AbstractBookMetaData {
    /**
     * Loads a sword config from a given Path.
     *
     * @param file
     *            the config file
     * @param bookRootPath
     *            the root path for the book
     * @throws IOException
     * @throws BookException
     *             indicates missing data files
     */
    @Throws(IOException::class, BookException::class)
    constructor(file: Path, bookRootPath: KUri?) : this(StorageLocation(file.toString()), bookRootPath)

    @Throws(IOException::class, BookException::class)
    internal constructor(file: StorageLocation, bookRootPath: KUri?) {
        this.installed = true
        this.configLocation = file
        this.bookConfPath = file.name // something like kjv.conf
        library = bookRootPath

        this.configAll = IniSection()
        this.filtered = true // Force it to run.
        reload(keyKeepers)
    }

    /**
     * Loads a sword config from a buffer gotten from mods.d.tar.gz or mods.d.zip.
     *
     * @param buffer
     * @param bookConf
     * @throws IOException
     * @throws BookException
     */
    @Throws(IOException::class, BookException::class)
    constructor(buffer: ByteArray?, bookConf: String?) {
        this.installed = false
        this.configLocation = null
        this.bookConfPath = bookConf // something like .../mods.d.zip!mods.d/kjv.conf
        this.supported = true
        this.configAll = IniSection()
        this.filtered = true // Force it to run.
        loadBuffer(buffer, keyKeepers)
        adjustConfig()
        report(configAll)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#reload()
     */
    @Throws(BookException::class)
    override fun reload() {
        reload(null)
    }

    @Throws(BookException::class)
    fun reload(keepers: Filter<String>?) {
        // Always run if it is filtered
        // Always run if a filter is supplied
        // Do not run if it is already not filtered and no filter is supplied
        if (!filtered && keepers == null) {
            return
        }
        this.supported = true
        if (configJSword != null) {
            configJSword!!.clear()
        }
        if (configFrontend != null) {
            configFrontend!!.clear()
        }
        try {
            if (installed) {
                loadFile(keepers)
            } else {
                val buffer = IOUtil.getZipEntry(bookConfPath!!)
                loadBuffer(buffer, keepers)
            }
            adjustConfig()
            report(configAll)

            this.configJSword = addConfig(MetaDataLocator.JSWORD)
            this.configFrontend = addConfig(MetaDataLocator.FRONTEND)
        } catch (ex: IOException) {
            throw BookException("unable to load conf", ex)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isQuestionable()
     */
    override val isQuestionable: Boolean
        get() =
            // some parameters don't support overrides
            questionable

    /* (non-Javadoc)
    * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#isSupported()
    */
    override val isSupported: Boolean
        get() =
            // The top most states whether the Book is supported
            supported

    /* (non-Javadoc)
    * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#isEnciphered()
    */
    override val isEnciphered: Boolean
        get() {
            val cipher = getProperty(KEY_CIPHER_KEY)
            return cipher != null
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#isLocked()
     */
    override val isLocked: Boolean
        get() {
            // A locked book is enciphered but without a key.
            val cipher = getProperty(KEY_CIPHER_KEY)
            return cipher != null && cipher.length == 0
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#unlock(java.lang.String)
     */
    override fun unlock(unlockKey: String?): Boolean {
        setProperty(KEY_CIPHER_KEY, unlockKey)
        return true
    }

    override fun resetLock() {
        setProperty(KEY_CIPHER_KEY, "")
    }

    /*
     * Can be overridden by front-end/jsword
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#getUnlockKey()
     */
    override val unlockKey: String?
        get() = getProperty(KEY_CIPHER_KEY)

    /*
     * Can be overridden by front-end/jsword
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getName()
     */
    override val name: String
        get() = getProperty(KEY_DESCRIPTION)!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getBookCharset()
     */
    override val bookCharset: String?
        get() {
            val encoding = getProperty(KEY_ENCODING) ?: return null
            return ENCODING_JAVA.get(encoding)
        }

    /* This value cannot be overridden by front-ends/jsword
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#getKeyType()
     */
    override val keyType: KeyType?
        get() {
            val bt = bookType ?: return null
            return bt.keyType
        }

    /**
     * The BookType for this Book
     *
     * @return the book type
     */
    var bookType: BookType? = null
        private set

    /**
     * @return the SourceFilter based upon the SourceType.
     */
    val filter: SourceFilter
        get() {
            val sourcetype = getProperty(KEY_SOURCE_TYPE)
            return sourceFilterFor(sourcetype)
        }

    /**
     * If the module's config is tied to a file remember it so that it can be
     * updated.
     *
     * To maintain backwards compatibility, this always returns the Sword conf
     * file Get the conf file for this SwordMetaData.
     *
     * @return Returns the conf file or null if loaded from a byte buffer.
     */
    val configFile: Path? get() = configLocation?.path

    internal val configLocation: StorageLocation?

    /**
     * Get the book conf which is a name such as kjv.conf.
     *
     * @return the book conf
     */
    val bookConf: String
        get() =
            // When loaded from a zip or tar.gz it is something like .../mods.d.zip!mods.d/kjv.conf
            // Otherwise it is merely kjv.conf.
            bookConfPath!!.substring(bookConfPath!!.lastIndexOf("/") + 1)

    /* Cannot be overridden by a front-end/jsword
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getBookCategory()
     */
    override val bookCategory: BookCategory
        get() {
            if (bookCat == null) {
                bookCat = getValue(BookMetaData.KEY_CATEGORY) as BookCategory?
                if (bookCat == BookCategory.OTHER) {
                    val bt = bookType
                    if (bt == null) {
                        return bookCat!!
                    }
                    bookCat = bt.bookCategory
                }
            }
            return bookCat!!
        }

    /* Cannot be overridden by a front-end/jsword
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#toOSIS()
     */
    override fun toOSIS(): Document = swordMetaDataToOsis(this)

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getInitials()
     */
    override val initials: String
        get() = configAll.name!!

    /**
     * @return the internal name of the module, useful when re-constructing all
     *         the meta-information, after installation for example
     */
    // was package-private
    internal val internalName: String?
        get() = configAll.name

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getAbbreviation()
     */
    override val abbreviation: String
        get() {
            val abbreviation = getProperty(KEY_ABBREVIATION)
            if (abbreviation != null && abbreviation.length > 0) {
                return abbreviation
            }
            return initials
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isLeftToRight()
     */
    override val isLeftToRight: Boolean
        get() {
            // This should return the dominate direction of the text, if it is BiDi,
            // then we have to guess.
            val dir = getProperty(KEY_DIRECTION)
            if (ConfigEntryType.DIRECTION_BIDI.equals(dir, ignoreCase = true)) {
                // When BiDi, return the dominate direction based upon the Book's
                // Language not Direction
                val lang = language
                return lang.isLeftToRight
            }

            return ConfigEntryType.DIRECTION_LTOR.equals(dir, ignoreCase = true)
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#hasFeature(org.crosswire.jsword.book.FeatureType)
     */
    override fun hasFeature(feature: FeatureType?): Boolean {
        val name = feature!!.toString()
        // Features are a positive statement.
        // If we find it mentioned anywhere, then it true
        if (configAll.containsValue(KEY_FEATURE, name)) {
            return true
        }
        // Many "features" are GlobalOptionFilters, which in the Sword C++ API
        // indicate a class to use for filtering.
        // These mostly have the source type prepended to the feature
        val buffer = StringBuilder(getProperty(KEY_SOURCE_TYPE)!!)
        buffer.append(name)
        if (configAll.containsValue(KEY_GLOBAL_OPTION_FILTER, buffer.toString())) {
            return true
        }

        // Check for the alias prefixed by the source type
        val alias = feature.alias
        buffer.setLength(0)
        buffer.append(getProperty(KEY_SOURCE_TYPE))
        buffer.append(alias)

        // But some do not
        return configAll.containsValue(KEY_GLOBAL_OPTION_FILTER, name) || configAll.containsValue(KEY_GLOBAL_OPTION_FILTER, buffer.toString())
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getPropertyKeys()
     */
    override val propertyKeys: Set<String>?
        get() = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getProperty(java.lang.String)
     */
    override fun getProperty(key: String?): String? {
        if (BookMetaData.KEY_LANGUAGE == key) {
            return language.name
        }
        return configAll.get(key, DEFAULTS[key])
    }

    override fun getValues(key: String?): Collection<String>? {
        return configAll.getValues(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#setProperty(java.lang.String, java.lang.String)
     */
    override fun setProperty(key: String?, value: String?) {
        configAll.replace(key, value)
    }

    fun removeProperty(key: String?) {
        configAll.remove(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#putProperty(java.lang.String, java.lang.String, boolean)
     */
    override fun putProperty(key: String?, value: String?, forFrontend: Boolean) {
        val mdl = if (forFrontend) MetaDataLocator.FRONTEND else MetaDataLocator.JSWORD
        putProperty(key, value, mdl)
    }

    /**
     * Allow specification of a specific SwordMetaDataLocator when saving a property.
     *
     * @param key the entry that we are saving
     * @param value the value of the entry
     * @param metaDataLocator Place to save - front end storage, shared storage, or don't save(transient)
     */
    fun putProperty(key: String?, value: String?, metaDataLocator: MetaDataLocator?) {
        // Set the property for all to see
        setProperty(key, value)

        // Properties are only saved for installed books.
        if (!installed) {
            return
        }

        val writeLocation = metaDataLocator!!.writeLocationValue
        // Properties are only saved if there is a place for saving
        if (writeLocation == null) {
            return
        }
        // persist property for future sessions if JSword or Front-end MetaDataLocator
        // Wait until the first write to actually create the appropriate internal storage.
        var config: IniSection? = null
        when (metaDataLocator) {
            MetaDataLocator.FRONTEND -> {
                if (this.configFrontend == null) {
                    this.configFrontend = IniSection(configAll.name)
                }
                config = this.configFrontend
            }
            MetaDataLocator.JSWORD -> {
                if (this.configJSword == null) {
                    this.configJSword = IniSection(configAll.name)
                }
                config = this.configJSword
            }
            MetaDataLocator.TRANSIENT -> {
            }
        }

        if (config != null) {
            config.replace(key, value)
            try {
                storageSave(config, storageChild(writeLocation, bookConfPath), bookCharset)
            } catch (ex: IOException) {
                LOGGER.error("Unable to save {}={}: conf file for [{}]; error={}", key, value, configAll.name, ex)
            }
        }
    }

    /**
     * Load the conf from a file.
     *
     * @param keepers
     *            the keys to keep. When null keep all
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun loadFile(keepers: Filter<String>?) {
        filtered = keepers != null

        configAll.clear()
        configAll.load(storageRead(configLocation!!), ENCODING_UTF8, keepers)
        val encoding = configAll.get(KEY_ENCODING)
        if (!ENCODING_UTF8.equals(encoding, ignoreCase = true)) {
            configAll.clear()
            configAll.load(storageRead(configLocation!!), ENCODING_LATIN1, keepers)
        }
    }

    /**
     * Load the conf from a buffer. This is used to load conf entries from the cached mods.d.tar.gz or mods.d.zip file.
     *
     * @param buffer
     *            the buffer to load
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun loadBuffer(buffer: ByteArray?, keepers: Filter<String>?) {
        filtered = keepers != null

        configAll.clear()
        configAll.load(buffer, ENCODING_UTF8, keepers)
        val encoding = configAll.get(KEY_ENCODING)
        if (!ENCODING_UTF8.equals(encoding, ignoreCase = true)) {
            configAll.clear()
            configAll.load(buffer, ENCODING_LATIN1, keepers)
        }
    }

    private fun addConfig(locator: MetaDataLocator): IniSection? {
        // The write location supersedes the read location
        var conf = storageChild(locator.writeLocationValue, bookConfPath)
        if (!storageExists(conf)) {
            conf = storageChild(locator.readLocationValue, bookConfPath)
        }

        if (storageExists(conf)) {
            // The additional confs have the same encoding as the SWORD conf.
            val encoding = getProperty(KEY_ENCODING)
            try {
                val config = IniSection()
                config.load(storageRead(conf), encoding)
                mergeConfig(config)
                return config
            } catch (e: IOException) {
                LOGGER.error("Unable to load conf {}:{}", conf, e)
            }
        }

        return null
    }

    private fun mergeConfig(config: IniSection) {
        for (key in config.keys) {
            val type = ConfigEntryType.fromString(key)
            for (value in config.getValues(key)!!) {
                if (type != null && type.mayRepeat()) {
                    if (!configAll.containsValue(key, value)) {
                        configAll.add(key, value)
                    }
                } else {
                    setProperty(key, value)
                }
            }
        }
    }

    /**
     * Gets a particular entry value by its type
     *
     * @param key of the entry
     * @return the requested value, the default (if there is no entry) or null
     *         (if there is no default)
     */
    private fun getValue(key: String): Any? {
        val type = ConfigEntryType.fromString(key)
        val ce = getProperty(key)
        if (type == null) {
            return ce
        }

        return if (ce == null) null else type.convert(ce)
    }

    @Throws(BookException::class)
    private fun adjustConfig() {
        adjustLocation()
        adjustLanguage()
        adjustBookType()
        adjustName()
        adjustHistory(configAll)
    }

    private fun adjustLanguage() {
        val lang = getProperty(BookMetaData.KEY_LANG)
        testLanguage(BookMetaData.KEY_LANG, lang)

        var langFrom = configAll.get(KEY_GLOSSARY_FROM)
        var langTo = configAll.get(KEY_GLOSSARY_TO)

        // If we have either langFrom or langTo, we are dealing with a glossary
        if (langFrom != null || langTo != null) {
            if (langFrom == null) {
                langFrom = lang
                setProperty(KEY_GLOSSARY_FROM, langFrom)
                LOGGER.warn("Missing data for [{}]. Assuming {}={}", configAll.name, KEY_GLOSSARY_FROM, langFrom)
            }
            testLanguage(KEY_GLOSSARY_FROM, langFrom)

            if (langTo == null) {
                langTo = Language.DEFAULT_LANG.givenSpecification
                setProperty(KEY_GLOSSARY_TO, langTo)
                LOGGER.warn("Missing data for [{}]. Assuming {}={}", configAll.name, KEY_GLOSSARY_TO, langTo)
            }
            testLanguage(KEY_GLOSSARY_TO, langTo)

            // At least one of the two languages should match the lang entry
            if (langFrom != lang && langTo != lang) {
                LOGGER.error("Data error in [{}]. Neither {} or {} match {}", configAll.name, KEY_GLOSSARY_FROM, KEY_GLOSSARY_TO, BookMetaData.KEY_LANG)
            }
        }

        language = getValue(BookMetaData.KEY_LANG) as Language
    }

    private fun testLanguage(key: String, lang: String?) {
        val language = Language(lang)
        if (!language.isValidLanguage) {
            LOGGER.warn("Unknown language [{}]{}={}", configAll.name, key, lang)
        }
    }

    private fun adjustBookType() {
        // The book type represents the underlying category of book.
        // Fine tune it here.
        var focusedCategory = getValue(BookMetaData.KEY_CATEGORY) as BookCategory?
        questionable = focusedCategory == BookCategory.QUESTIONABLE

        val modTypeName = getProperty(KEY_MOD_DRV)
        if (modTypeName == null) {
            LOGGER.error("Book not supported: malformed conf file for [{}] no {} found.", configAll.name, KEY_MOD_DRV)
            supported = false
            return
        }

        val v11n = getProperty(BookMetaData.KEY_VERSIFICATION)
        if (!Versifications.instance().isDefined(v11n)) {
            LOGGER.error("Book not supported: Unknown versification for [{}]{}={}.", configAll.name, BookMetaData.KEY_VERSIFICATION, v11n)
            supported = false
            return
        }

        bookType = BookType.fromString(modTypeName)
        if (bookType == null) {
            LOGGER.error("Book not supported: malformed conf file for [{}] no book type {} found", configAll.name, modTypeName)
            supported = false
            return
        }

        // The book type represents the underlying category of book.
        // Fine tune it here.
        if (focusedCategory == BookCategory.OTHER) {
            focusedCategory = bookType!!.bookCategory
        }

        setProperty(BookMetaData.KEY_CATEGORY, focusedCategory!!.getName())
    }

    private fun adjustName() {
        // If there is no name then use the initials name
        if (configAll.get(KEY_DESCRIPTION) == null) {
            LOGGER.error("Malformed conf file: missing [{}]{}=. Using {}", configAll.name, KEY_DESCRIPTION, configAll.name)
            setProperty(KEY_DESCRIPTION, configAll.name)
        }
    }

    /* This method sets the location on the sword conf file for an installed Book.
     */
    @Throws(BookException::class)
    private fun adjustLocation() {

        val library = this.library
        if (library == null) {
            return
        }

        // Previously, all DATA_PATH entries end in / to indicate dirs
        // or not to indicate file prefixes.
        // This is no longer true.
        // Now we need to test the file/url to see if it exists and is a
        // directory.
        var datapath = getProperty(KEY_DATA_PATH)
        if (datapath == null) {
            throw BookException("Data path for the book ${configAll.name} is not set")
        }
        var lastSlash = datapath.lastIndexOf('/')

        // There were modules that did not have a valid DataPath.
        // This should not be necessary
        if (lastSlash == -1) {
            return
        }

        // DataPath typically ends in a '/' to indicate a directory.
        // If so remove it.
        var isDirectoryPath = false
        if (lastSlash == datapath.length - 1) {
            isDirectoryPath = true
            datapath = datapath.substring(0, lastSlash)
        }

        var location = NetUtil.lengthenURI(library, datapath)
        val bookDir = StorageLocation(location!!.getPath()!!)
        // For some modules, the last element of the DataPath
        // is a prefix for file names.
        if (!storageDirectory(bookDir)) {
            if (isDirectoryPath) {
                // TRANSLATOR: This indicates that the Book is only partially installed.
                throw BookException(JSMsg.gettext("The book {0} is missing its data files", configAll.name))
            }

            // not a directory path
            // try appending .dat on the end to see if we have a file, if not,
            // then
            if (!storageExists(StorageLocation(location.getPath() + ".dat"))) {
                // TRANSLATOR: This indicates that the Book is only partially
                // installed.
                throw BookException(JSMsg.gettext("The book {0} is missing its data files", configAll.name))
            }

            // then we have a module that has a prefix
            // Shorten it by one segment and test again.
            lastSlash = datapath.lastIndexOf('/')
            datapath = datapath.substring(0, lastSlash)
            location = NetUtil.lengthenURI(library, datapath)
        }

        this.location = location
    }

    /**
     * Indicates whether the Book is installed or not.
     */
    private var installed = false

    /**
     * When true this BookMetaData is filtered and only partially loaded.
     * Reloading without a filter will change this to false.
     */
    private var filtered = false

    /**
     * The name of the conf file, such as kjv.conf.
     */
    private var bookConfPath: String? = null

    /**
     * The configAll IniSection holds the merged view of the SWORD config,
     * configJSword, and configFrontend.
     */
    internal val osisConfig: IniSection
        get() = configAll

    private val configAll: IniSection

    /**
     * configJSword holds shared configuration for all front-ends.
     */
    private var configJSword: IniSection? = null

    /**
     * configFrontend contains the configuration for the current front-end.
     */
    private var configFrontend: IniSection? = null

    /**
     * True if this book's config type can be used by JSword.
     */
    private var supported = false

    /**
     * The BookCategory for this Book
     */
    private var bookCat: BookCategory? = null

    /**
     * True if this book is considered questionable.
     */
    private var questionable = false

    /**
     * KeyFilter returns true for keys that should always be present.
     * A partially loaded SwordBookMetaData will satisfy this filter.
     */
    private class KeyFilter
    /**
     * Create a KeyFilter for the expected keys.
     * @param keepers the list of keys that should be retained
     */
    (keepers: Array<String>) : Filter<String> {
        override fun test(obj: String): Boolean {
            return keepers.contains(obj) || obj.startsWith("AndBible")
        }

        private val keepers: MutableSet<String> = mutableSetOf()

        init {
            // Load up the keepers set
            for (key in keepers) {
                this.keepers.add(key)
            }
        }
    }

    companion object {
        // These plus a few from BookMetaData are the only ones defined by SWORD
        // These ones from BookMetaData are:
        // KEY_VERSIFICATION
        // KEY_FONT
        // KEY_LANG
        // KEY_CATEGORY
        // Two others are for JSword and are defined in BookMetaData:
        // KEY_SCOPE
        // KEY_BOOKLIST
        const val KEY_ABBREVIATION = "Abbreviation"
        const val KEY_ABOUT = "About"
        const val KEY_BLOCK_COUNT = "BlockCount"
        const val KEY_BLOCK_TYPE = "BlockType"
        const val KEY_CASE_SENSITIVE_KEYS = "CaseSensitiveKeys"
        const val KEY_CIPHER_KEY = "CipherKey"
        const val KEY_COMPRESS_TYPE = "CompressType"
        const val KEY_COPYRIGHT = "Copyright"
        const val KEY_COPYRIGHT_CONTACT_ADDRESS = "CopyrightContactAddress"
        const val KEY_COPYRIGHT_CONTACT_EMAIL = "CopyrightContactEmail"
        const val KEY_COPYRIGHT_CONTACT_NAME = "CopyrightContactName"
        const val KEY_COPYRIGHT_CONTACT_NOTES = "CopyrightContactNotes"
        const val KEY_COPYRIGHT_DATE = "CopyrightDate"
        const val KEY_COPYRIGHT_HOLDER = "CopyrightHolder"
        const val KEY_COPYRIGHT_NOTES = "CopyrightNotes"
        const val KEY_DATA_PATH = "DataPath"
        const val KEY_DESCRIPTION = "Description"
        const val KEY_DIRECTION = "Direction"
        const val KEY_DISPLAY_LEVEL = "DisplayLevel"
        const val KEY_DISTRIBUTION_LICENSE = "DistributionLicense"
        const val KEY_DISTRIBUTION_NOTES = "DistributionNotes"
        const val KEY_DISTRIBUTION_SOURCE = "DistributionSource"
        const val KEY_ENCODING = "Encoding"
        const val KEY_FEATURE = "Feature"
        const val KEY_GLOBAL_OPTION_FILTER = "GlobalOptionFilter"
        const val KEY_SIGLUM1 = "Siglum1"
        const val KEY_SIGLUM2 = "Siglum2"
        const val KEY_SIGLUM3 = "Siglum3"
        const val KEY_SIGLUM4 = "Siglum4"
        const val KEY_SIGLUM5 = "Siglum5"
        const val KEY_GLOSSARY_FROM = "GlossaryFrom"
        const val KEY_GLOSSARY_TO = "GlossaryTo"
        const val KEY_HISTORY = "History"
        const val KEY_INSTALL_SIZE = "InstallSize"
        const val KEY_KEY_TYPE = "KeyType"
        const val KEY_LCSH = "LCSH"
        const val KEY_LOCAL_STRIP_FILTER = "LocalStripFilter"
        const val KEY_MINIMUM_VERSION = "MinimumVersion"
        const val KEY_MOD_DRV = "ModDrv"
        const val KEY_OBSOLETES = "Obsoletes"
        const val KEY_OSIS_Q_TO_TICK = "OSISqToTick"
        const val KEY_OSIS_VERSION = "OSISVersion"
        const val KEY_PREFERRED_CSS_XHTML = "PreferredCSSXHTML"
        const val KEY_SEARCH_OPTION = "SearchOption"
        const val KEY_SHORT_COPYRIGHT = "ShortCopyright"
        const val KEY_SHORT_PROMO = "ShortPromo"
        const val KEY_SOURCE_TYPE = "SourceType"
        const val KEY_STRONGS_PADDING = "StrongsPadding"
        const val KEY_SWORD_VERSION_DATE = "SwordVersionDate"
        const val KEY_TEXT_SOURCE = "TextSource"
        const val KEY_UNLOCK_URL = "UnlockURL"
        const val KEY_UNLOCK_INFO = "UnlockInfo"
        const val KEY_VERSION = "Version"



        // Some keys have defaults
        @JvmField
        val DEFAULTS: Map<String, String>

        init {
            val tempMap = mutableMapOf<String, String>()
            tempMap.put(KEY_COMPRESS_TYPE, "LZSS")
            tempMap.put(KEY_BLOCK_TYPE, "CHAPTER")
            tempMap.put(KEY_BLOCK_COUNT, "200")
            tempMap.put(KEY_KEY_TYPE, "TreeKey")
            tempMap.put(BookMetaData.KEY_VERSIFICATION, "KJV")
            tempMap.put(KEY_DIRECTION, "LtoR")
            tempMap.put(KEY_SOURCE_TYPE, "Plaintext")
            tempMap.put(KEY_ENCODING, "Latin-1")
            tempMap.put(KEY_DISPLAY_LEVEL, "1")
            tempMap.put(KEY_OSIS_Q_TO_TICK, "true")
            tempMap.put(KEY_VERSION, "1.0")
            tempMap.put(KEY_MINIMUM_VERSION, "1.5.1a")
            tempMap.put(BookMetaData.KEY_CATEGORY, "Other")
            tempMap.put(BookMetaData.KEY_LANG, "en")
            tempMap.put(KEY_DISTRIBUTION_LICENSE, "Public Domain")
            tempMap.put(KEY_CASE_SENSITIVE_KEYS, "false")
            tempMap.put(KEY_STRONGS_PADDING, "true")
            DEFAULTS = metadataDefaultsView(tempMap)
        }

        /**
         * Allow for partial loading of a minimum set of keys, saving time and space.
         * If partial, call reload(null) to fill it in before showing the conf contents to a user.
         *
         * @param partial
         */
        @JvmStatic
        fun setPartialLoading(partial: Boolean) {

            if (partial != partialLoading) {
                if (partial) {
                    keyKeepers = KeyFilter(REQUIRED)
                } else {
                    keyKeepers = null
                }
            }
            partialLoading = partial
        }

        // History is a special case. It is of the form History_x.x
        // The ConfigEntryType is History without the _x.x.
        // We want to put x.x at the beginning of the string
        internal fun adjustHistory(config: IniSection) {
            // Iterate over a copy of the keys so that we don't get
            // a concurrent modification exception when we remove matching keys
            // and when we add new keys
            val keys: List<String> = config.keys.toList()
            for (key in keys) {
                var value = config.get(key)
                val type = ConfigEntryType.fromString(key)
                if (ConfigEntryType.HISTORY == type) {
                    config.remove(key)
                    val pos = key.indexOf('_')
                    value = key.substring(pos + 1) + ' ' + value
                    config.add(KEY_HISTORY, value)
                }
            }
        }

        @JvmStatic
        fun report(config: IniSection) {
            val buf = StringBuilder(config.report()!!)
            for (key in config.keys) {
                var type = ConfigEntryType.fromString(key)

                if (type == null) {
                    if (key.contains("_")) {
                        val baseKey = key.substring(0, key.indexOf('_'))
                        type = ConfigEntryType.fromString(baseKey)
                    }
                }


                val count = config.size(key)
                for (i in 0 until count) {
                    var value = config.get(key, i)

                    if (key.startsWith("AndBible")) {
                        // Disable all checking for AndBible custom keys.
                        continue
                    }
                    // If it is still unknown, report and skip
                    if (type == null) {
                        buf.append("Unknown entry: ").append(key).append(" = ").append(value).append('\n')
                        continue
                    }

                    // Only CIPHER_KEYS that are empty are not ignored
                    if (value!!.length == 0 && type != ConfigEntryType.CIPHER_KEY) {
                        buf.append("Unexpected empty entry: ").append(key).append(" = ").append(value).append('\n')
                        continue
                    }

                    // Filter known types of entries
                    value = type.filter(value)

                    // Report on fields that shouldn't have RTF but do
                    if (!type.allowsRTF() && RTF_PATTERN.containsMatchIn(value!!)) {
                        buf.append("Unexpected RTF: ").append(key).append(" = ").append(value).append('\n')
                    }

                    if (!type.allowsHTML() && HTML_PATTERN.containsMatchIn(value!!)) {
                        buf.append("Unexpected HTML: ").append(key).append(" = ").append(value).append('\n')
                    }

                    if (!type.isAllowed(value)) {
                        buf.append("Unknown config value: ").append(key).append(" = ").append(value).append('\n')
                    }

                    if (count > 1 && !type.mayRepeat()) {
                        buf.append("Unexpected repeated config key: ").append(key).append(" = ").append(value).append('\n')
                    }
                }
            }
            if (buf.length > 0) {
                LOGGER.info("Conf report for [{}]\n{}", config.name, buf.toString())
            }
        }

        /**
         * These are the elements that JSword uses for control when present in a conf.
         */
        private val REQUIRED = arrayOf(
            KEY_ABBREVIATION,
            KEY_DESCRIPTION,
            BookMetaData.KEY_LANG,
            BookMetaData.KEY_CATEGORY,
            KEY_VERSION,
            KEY_FEATURE,
            KEY_GLOBAL_OPTION_FILTER,
            KEY_SIGLUM1,
            KEY_SIGLUM2,
            KEY_SIGLUM3,
            KEY_SIGLUM4,
            KEY_SIGLUM5,
            BookMetaData.KEY_FONT,
            KEY_DATA_PATH,
            KEY_MOD_DRV,
            KEY_SOURCE_TYPE,
            KEY_BLOCK_TYPE,
            KEY_BLOCK_COUNT,
            KEY_COMPRESS_TYPE,
            KEY_ENCODING,
            KEY_DIRECTION,
            KEY_KEY_TYPE,
            KEY_DISPLAY_LEVEL,
            BookMetaData.KEY_VERSIFICATION,
            KEY_CASE_SENSITIVE_KEYS,
            KEY_LOCAL_STRIP_FILTER,
            KEY_PREFERRED_CSS_XHTML,
            KEY_STRONGS_PADDING,
            KEY_SEARCH_OPTION,
            KEY_INSTALL_SIZE,
            BookMetaData.KEY_SCOPE,
            BookMetaData.KEY_BOOKLIST,
            KEY_CIPHER_KEY
        )

        private var partialLoading = false

        private var keyKeepers: Filter<String>? = null

        internal val OSIS_INFO = arrayOf(
            KEY_ABBREVIATION,
            KEY_DESCRIPTION,
            BookMetaData.KEY_LANG,
            BookMetaData.KEY_CATEGORY,
            KEY_LCSH,
            KEY_SWORD_VERSION_DATE,
            KEY_VERSION,
            KEY_HISTORY,
            KEY_OBSOLETES,
            KEY_GLOSSARY_FROM,
            KEY_GLOSSARY_TO,
            KEY_ABOUT,
            KEY_SHORT_PROMO,
            KEY_DISTRIBUTION_LICENSE,
            KEY_DISTRIBUTION_NOTES,
            KEY_DISTRIBUTION_SOURCE,
            KEY_SHORT_COPYRIGHT,
            KEY_COPYRIGHT,
            KEY_COPYRIGHT_DATE,
            KEY_COPYRIGHT_HOLDER,
            KEY_COPYRIGHT_CONTACT_NAME,
            KEY_COPYRIGHT_CONTACT_ADDRESS,
            KEY_COPYRIGHT_CONTACT_EMAIL,
            KEY_COPYRIGHT_CONTACT_NOTES,
            KEY_COPYRIGHT_NOTES,
            KEY_TEXT_SOURCE,
            KEY_FEATURE,
            KEY_GLOBAL_OPTION_FILTER,
            KEY_SIGLUM1,
            KEY_SIGLUM2,
            KEY_SIGLUM3,
            KEY_SIGLUM4,
            KEY_SIGLUM5,
            BookMetaData.KEY_FONT,
            KEY_DATA_PATH,
            KEY_MOD_DRV,
            KEY_SOURCE_TYPE,
            KEY_BLOCK_TYPE,
            KEY_BLOCK_COUNT,
            KEY_COMPRESS_TYPE,
            KEY_ENCODING,
            KEY_MINIMUM_VERSION,
            KEY_OSIS_VERSION,
            KEY_OSIS_Q_TO_TICK,
            KEY_DIRECTION,
            KEY_KEY_TYPE,
            KEY_DISPLAY_LEVEL,
            BookMetaData.KEY_VERSIFICATION,
            KEY_CASE_SENSITIVE_KEYS,
            KEY_LOCAL_STRIP_FILTER,
            KEY_PREFERRED_CSS_XHTML,
            KEY_STRONGS_PADDING,
            KEY_SEARCH_OPTION,
            KEY_INSTALL_SIZE,
            BookMetaData.KEY_SCOPE,
            BookMetaData.KEY_BOOKLIST
        )

        internal val HIDDEN = arrayOf(
            KEY_CIPHER_KEY,
            BookMetaData.KEY_LANGUAGE
        )

        private val RTF_PATTERN: Regex = Regex("\\\\pard|\\\\pa[er]|\\\\qc|\\\\[bi]|\\\\u-?[0-9]{4,6}+")
        private val HTML_PATTERN: Regex = Regex("(<[a-zA-Z]|[a-zA-Z]>)")

        /**
         * Sword only recognizes two encodings for its modules: UTF-8 and Latin-1
         * Sword uses MS Windows cp1252 for Latin 1 not the standard.
         * Arrgh! The encoding strings need to be converted to Java charsets
         */
        private const val ENCODING_UTF8 = "UTF-8"
        private const val ENCODING_LATIN1 = "WINDOWS-1252"
        private val ENCODING_JAVA = PropertyMap()

        init {
            ENCODING_JAVA.put("Latin-1", ENCODING_LATIN1)
            ENCODING_JAVA.put("UTF-8", ENCODING_UTF8)
        }

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(SwordBookMetaData::class)
    }
}
