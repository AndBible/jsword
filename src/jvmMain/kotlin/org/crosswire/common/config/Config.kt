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
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
/**
 * <p>Config allows an application to separate the responsibilities of setting
 * itself up, and providing a user interface from the real work that it needs
 * to do.</p>
 *
 * <h2>Introduction</h2>
 * <p>Config is (mostly) all kept in a few packages in the util source tree. The
 *  design aims for the following goals:</p>
 * <ul>
 *   <li>Application Transparency - It should be possible to add a configuration
 *     dialog to an application without adding hundreds of hooks either to your
 *     application to read the current state, or to the configuration system to
 *     work with the application. This is achieved via an xml config file and a
 *     healthy dose of reflection.</li>
 *   <li>View Independance - Currently there are a number of Swing front ends -
 *     a Mozilla style config dialog with a tree, a more conventional tabbed dialog,
 *     and a prototype wizard style interface. There has also been a servlet front-end
 *     however the code to do this has suffered bit-rot, and should not be considered
 *     useful. It does however prove the view independance concept.</li>
 * </ul>
 *
 * <h3>How To Use Config</h3>
 * <p>There are a number of simple steps. First a config.xml file is needed to tell
 * the config system what to configure and how.</p>
 *
 * <pre>
 * &lt;config&gt;
 *
 *   <span style="color: #336600">&lt;!-- A configuration is a set of options ... --&gt;</span>
 *   <span style="color: #336600">&lt;!-- The key is a dot separated name - Imaging this in a Mozilla tree or some nested tabs. --&gt;</span>
 *   &lt;option key=&quot;Bibles.Sword.Base Directory&quot; type=&quot;string&quot;&gt;
 *     <span style="color: #336600">&lt;!-- The type (above) along with the introspect line configures what JavaBean methods will be called --&gt;</span>
 *     &lt;introspect class=&quot;org.crosswire.jsword.book.sword.SwordBibleDriver&quot; property=&quot;SwordDir&quot;/&gt;
 *     <span style="color: #336600">&lt;!-- The tool-tip (or similar) describing what is going on --&gt;</span>
 *     &lt;help&gt;Where is the SWORD Project base directory.&lt;/help&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- Another option, this time it is a boolean option which will show up as a tickbox --&gt;</span>
 *   &lt;option key=&quot;Bibles.Display.Persistent Naming&quot; level=&quot;advanced&quot; type=&quot;boolean&quot;&gt;
 *     &lt;introspect class=&quot;org.crosswire.jsword.passage.PassageUtil&quot; property=&quot;PersistentNaming&quot;/&gt;
 *     &lt;help&gt;True if the passage editor re-writes the references to conform to its notation.&lt;/help&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- Another type again this one for the look and feel. --&gt;
 *   &lt;!-- The reason for the helper class here is to alter windows that are not currently mapped --&gt;</span>
 *     &lt;option key=&quot;Looks.Look and Feel&quot; type=&quot;class&quot;&gt;
 *     &lt;introspect class=&quot;org.crosswire.common.swing.LookAndFeelUtil&quot; property=&quot;LookAndFeel&quot;/&gt;
 *     &lt;help&gt;The look and feel of the application&lt;/help&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- When we have have an Enum style config option ... --&gt;</span>
 *     &lt;option key=&quot;Bibles.Display.Book Case&quot; level=&quot;advanced&quot; type=&quot;int-options&quot;&gt;
 *     &lt;introspect class=&quot;org.crosswire.jsword.passage.Books&quot; property=&quot;Case&quot;/&gt;
 *     &lt;help&gt;What case should we use to display the references.&lt;/help&gt;
 *     &lt;alternative number=&quot;0&quot; name=&quot;lower&quot;/&gt;
 *     &lt;alternative number=&quot;1&quot; name=&quot;Sentence&quot;/&gt;
 *     &lt;alternative number=&quot;2&quot; name=&quot;UPPER&quot;/&gt;
 *     &lt;alternative number=&quot;3&quot; name=&quot;mIXeD&quot;/&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- The options here are more complex and need to be provided as a string array by Java code (see below) --&gt;</span>
 *     &lt;option key=&quot;Bibles.Default&quot; type=&quot;string-options&quot;&gt;
 *     &lt;introspect class=&quot;org.crosswire.jsword.book.Bibles&quot; property=&quot;DefaultName&quot;/&gt;
 *     &lt;help&gt;Which of the available Bibles is the default.&lt;/help&gt;
 *     &lt;map name=&quot;biblenames&quot;/&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- This option is 'advanced' which means it is not visible to all users (see below) --&gt;</span>
 *     &lt;option key=&quot;Advanced.Source Path&quot; level=&quot;advanced&quot; type=&quot;path&quot;&gt;
 *     &lt;introspect class=&quot;org.crosswire.common.swing.DetailedExceptionPane&quot; property=&quot;SourcePath&quot;/&gt;
 *     &lt;help&gt;The directories to search for source code in when investigating an exception.&lt;/help&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- When the choice is very custom you can always do your own implementation --&gt;
 *   &lt;!-- This allows us to set users levels so not everyone gets asked hard questions --&gt;</span>
 *     &lt;option key=&quot;Advanced.User Level&quot; type=&quot;custom&quot; class=&quot;org.crosswire.common.util.UserLevel$UserLevelChoice&quot;&gt;
 *     &lt;help&gt;How advanced is your knowledge of this program.&lt;/help&gt;
 *   &lt;/option&gt;
 *
 *   <span style="color: #336600">&lt;!-- There are other examples in config.xml --&gt;</span>
 * &lt;/config&gt;
 * </pre>
 *
 * <p>Then you need to add the Java code:</p>
 * <pre><span style="color: #336600">
 * // To load the config.xml file:
 * </span>Config config = new Config(&quot;Tool Shed Options&quot;);
 * Document xmlconfig = Project.resource().getDocument(&quot;config&quot;); <span style="color: #336600">// Or whatever to get a JDOM Document</span>
 * config.add(xmlconfig);
 *
 * <span style="color: #336600">// To load a saved config</span>
 * config.setProperties(Project.resource().getProperties(&quot;desktop&quot;)); <span style="color: #336600">// Or however you get a Properties</span>
 * config.localToApplication(true);
 *
 * <span style="color: #336600">// And display it ...</span>
 * URL configurl = Project.resource().getPropertiesURL(&quot;desktop&quot;); <span style="color: #336600">// URL of the Properties file to save to</span>
 * SwingConfig.showDialog(config, parentWind, configurl);
 *
 * <span style="color: #336600">// The code above needed help in setting up a string choice. This is how ...</span>
 * ChoiceFactory.getDataMap().put(&quot;biblenames&quot;, Bibles.getBibleNames());
 * </pre>
 * <p>There are more examples in <code>org.crosswire.bibledesktop.desktop.OptionsAction.</code></p>
 */
package org.crosswire.common.config

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.IOException
import java.net.URI
import java.util.ResourceBundle
import java.util.concurrent.CopyOnWriteArrayList
import org.crosswire.common.util.LucidException
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.PropertyMap
import org.crosswire.common.util.Reporter
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg
import org.jdom2.Document
import org.slf4j.LoggerFactory

/**
 * Config is the core part of the configuration system; it is simply a
 * Collection of <code>Choice</code>s.
 * 
 * Config does the following things:
 * <ul>
 * <li>Provides a GUI independent API with which to create GUIs</li>
 * <li>Stores a local store of settings</li>
 * <li>Allows updates to the local store</li>
 * </ul>
 * 
 * 
 * Config does not attempt to make permanent copies of the config data because
 * different applications may wish to store the data in different ways. Possible storage
 * mechanisms include:
 * <ul>
 * <li>Properties Files</li>
 * <li>Resource Objects (J2SE 1.4)</li>
 * <li>Network Sockets (see Remote)</li>
 * </ul>
 * 
 * The Config class stored the current Choices, and moves the data between the
 * various places that it is stored. There are 4 storage areas:
 * <ul>
 * <li><b>Permanent:</b> This can be local file, a URI, or a remote server Data
 * is stored here between invocations of the program.
 * <li><b>Application:</b> This is the actual working copy of the data.
 * <li><b>Screen:</b> This copy of the data is shown on screen whist a Config
 * dialog box is showing.
 * <li><b>Local:</b> This is required so that we can tell which bits of data
 * have been changed in the screen data, and so that we can load data from disk
 * to screen without involving the app.
 * </ul>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class Config
/**
 * Config ctor
 * 
 * @param title
 *            The name for dialog boxes and properties files
 */
(
    /**
     * The name for dialog boxes and properties files
     */
    @JvmField
    protected var title: String?
) : Iterable<Choice> {
    /**
     * The name for the dialog boxes and properties files.
     * 
     * @return the title for this config
     */
    // A function: the protected field has the name of the property
    open fun getTitle(): String? {
        return title
    }

    /**
     * Add a key/model pairing
     * 
     * @param model
     *            The Choice model to map to its key
     */
    open fun add(model: Choice) {
        val key = model.key
        // log.debug("Adding key={}", key);

        keys.add(key!!)
        models.add(model)

        var value = model.string
        if (value == null) {
            value = ""
            LOGGER.info("key={} had a null value", key)
        }

        putLocal(key, value)

        fireChoiceAdded(key, model)
    }

    /**
     * Add the set of configuration options specified in the xml file.
     * 
     * @param xmlconfig
     *            The JDOM document to read.
     * @param configResources
     *            contains the user level text for this config
     */
    open fun add(xmlconfig: Document, configResources: ResourceBundle?) {
        // We are going to assume a DTD has validated the config file and
        // just assume that everything is laid out properly.
        val root = xmlconfig.rootElement
        val iter = root.children.iterator()
        while (iter.hasNext()) {
            val element = iter.next()
            val key = element.getAttributeValue("key")

            var ex: Exception? = null
            try {
                val choice = ChoiceFactory.getChoice(element, configResources!!)
                if (!choice.isIgnored) {
                    add(choice)
                }
            } catch (e: StartupException) {
                ex = e
            } catch (e: ReflectiveOperationException) {
                ex = e
            }

            if (ex != null) {
                LOGGER.warn("Error creating config element, key={}", key, ex)
            }
        }
    }

    /**
     * Remove a key/model pairing
     * 
     * @param key
     *            The name to kill
     */
    open fun remove(key: String?) {
        val model = getChoice(key)
        keys.remove(key)
        models.remove(model)

        // Leave the pair in local?
        // local.put(key, value);

        fireChoiceRemoved(key, model)
    }

    /**
     * The set of Choice that we are controlling
     * 
     * @return An iterator over the choices
     */
    override fun iterator(): Iterator<Choice> {
        return models.iterator()
    }

    /**
     * Get the Choice for a given key
     * 
     * @param key the key for the choice
     * @return the requested choice
     */
    open fun getChoice(key: String?): Choice? {
        val index = keys.indexOf(key)
        if (index == -1) {
            return null
        }

        return models.get(index)
    }

    /**
     * The number of Choices
     * 
     * @return The number of Choices
     */
    open fun size(): Int {
        return keys.size
    }

    /**
     * Set a configuration Choice (by name) to a new value. This method is only
     * of use to classes displaying config information.
     * 
     * @param name the key for the choice
     * @param value the value for the choice
     */
    open fun setLocal(name: String?, value: String?) {
        jswordAssert(name != null) { "" }
        jswordAssert(value != null) { "" }

        putLocal(name, value)
    }

    /**
     * Get a configuration Choice (by name). This method is only of use to
     * classes displaying config information.
     * 
     * @param name the key for the choice
     * @return the value for the choice.
     */
    open fun getLocal(name: String): String? {
        return local.get(name)
    }

    /**
     * Take the data in the application and copy it to the local storage area.
     */
    open fun applicationToLocal() {
        for (key in keys) {
            val model = getChoice(key)!!
            val value = model.string
            putLocal(key, value)
        }
    }

    /**
     * Take the data in the local storage area and copy it to the application.
     */
    open fun localToApplication() {
        for (key in keys) {
            val choice = getChoice(key)!!

            val oldValue = choice.string // never returns null
            var newValue = local.get(key)

            // The new value shouldn't really be blank - obviously this
            // choice has just been added, substitute the default.
            if ((newValue == null) || (newValue.length == 0)) {
                if ((oldValue == null) || (oldValue.length == 0)) {
                    continue
                }
                putLocal(key, oldValue)
                newValue = oldValue
            }

            // If a value has not changed, we only call setString()
            // if force==true or if a higher priority choice has
            // changed.
            if (newValue != oldValue) {
                LOGGER.info("Setting {}={} (was {})", key, newValue, oldValue)
                try {
                    choice.string = newValue
                    changeListeners?.firePropertyChange(PropertyChangeEvent(choice, choice.key, oldValue, newValue))
                } catch (ex: LucidException) {
                    LOGGER.warn("Failure setting {}={}", key, newValue, ex)
                    Reporter.informUser(this, ConfigException(JSOtherMsg.lookupText("Failed to set option: {0}", choice.fullPath), ex))
                }
            }
        }
    }

    /**
     * Take the data stored permanently and copy it to the local storage area,
     * using the specified stream.
     * 
     * @param prop the set of properties to save
     */
    open fun setProperties(prop: PropertyMap) {
        for (key in prop.keys) {
            val value = prop.get(key)

            val model = getChoice(key)
            // Only if a value was stored and it should be stored then we use
            // it.
            if (value != null && model != null && model.isSaveable) {
                putLocal(key, value)
            }
        }
    }

    /**
     * Take the data in the local storage area and store it permanently.
     * 
     * @return the collection of properties
     */
    @Suppress("UNCHECKED_CAST")
    open fun getProperties(): PropertyMap {
        val prop = PropertyMap()

        for (key in keys) {
            val value = local.get(key)

            val model = getChoice(key)!!
            if (model.isSaveable) {
                (prop as MutableMap<String, String?>).put(key, value)
            } else {
                prop.remove(key)
            }
        }

        return prop
    }

    /**
     * Take the data stored permanently and copy it to the local storage area,
     * using the configured storage area
     * 
     * @param uri the location of the permanent storage
     * @throws IOException if there was a problem getting the permanent config info
     */
    @Throws(IOException::class)
    open fun permanentToLocal(uri: URI) {
        setProperties(NetUtil.loadProperties(uri))
    }

    /**
     * Take the data in the local storage area and store it permanently, using
     * the configured storage area.
     * 
     * @param uri the location of the permanent storage
     * @throws IOException if there was a problem storing the permanent config info
     */
    @Throws(IOException::class)
    open fun localToPermanent(uri: URI) {
        NetUtil.storeProperties(getProperties(), uri, title)
    }

    /**
     * Add a PropertyChangeListener to the listener list. The listener is
     * registered for all properties.
     * 
     * @param listener
     *            The PropertyChangeListener to be added
     */
    open fun addPropertyChangeListener(listener: PropertyChangeListener?) {
        var support = changeListeners
        if (support == null) {
            support = PropertyChangeSupport(this)
            changeListeners = support
        }
        support.addPropertyChangeListener(listener)
    }

    /**
     * Add a PropertyChangeListener for a specific property. The listener will
     * be invoked only when a call on firePropertyChange names that specific
     * property.
     * 
     * @param propertyName
     *            The name of the property to listen on.
     * @param listener
     *            The PropertyChangeListener to be added
     */
    open fun addPropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) {
        var support = changeListeners
        if (support == null) {
            support = PropertyChangeSupport(this)
            changeListeners = support
        }
        support.addPropertyChangeListener(propertyName, listener)
    }

    /**
     * Remove a PropertyChangeListener from the listener list. This removes a
     * PropertyChangeListener that was registered for all properties.
     * 
     * @param listener
     *            The PropertyChangeListener to be removed
     */
    open fun removePropertyChangeListener(listener: PropertyChangeListener?) {
        changeListeners?.removePropertyChangeListener(listener)
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     * 
     * @param propertyName
     *            The name of the property that was listened on.
     * @param listener
     *            The PropertyChangeListener to be removed
     */
    open fun removePropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) {
        changeListeners?.removePropertyChangeListener(propertyName, listener)
    }

    /**
     * Add an Exception listener to the list of things wanting to know whenever
     * we capture an Exception.
     * 
     * @param li
     *            The ConfigListener to be added
     */
    open fun addConfigListener(li: ConfigListener) {
        listeners.add(li)
    }

    /**
     * Remove an Exception listener from the list of things wanting to know
     * whenever we capture an Exception
     * 
     * @param li
     *            The ConfigListener to be removed
     */
    open fun removeConfigListener(li: ConfigListener) {
        listeners.remove(li)
    }

    /**
     * A Choice got added.
     * 
     * @param key the key of the choice that has been added
     * @param model the choice that was added
     */
    protected open fun fireChoiceAdded(key: String?, model: Choice?) {
        val ev = ConfigEvent(this, key, model)
        for (listener in listeners) {
            listener.choiceAdded(ev)
        }
    }

    /**
     * A Choice got removed.
     * 
     * @param key the key of the choice that has been removed
     * @param model the choice that was removed
     */
    protected open fun fireChoiceRemoved(key: String?, model: Choice?) {
        val ev = ConfigEvent(this, key, model)
        for (listener in listeners) {
            listener.choiceRemoved(ev)
        }
    }

    /**
     * Put a value into the local store; PropertyMap is a Map of String to String, but
     * the Java code stored whatever it was given, including null.
     */
    @Suppress("UNCHECKED_CAST")
    private fun putLocal(key: String?, value: String?) {
        (local as MutableMap<String?, String?>).put(key, value)
    }

    /**
     * The array that stores the keys
     */
    @JvmField
    protected var keys: MutableList<String> = ArrayList()

    /**
     * The array that stores the models
     */
    @JvmField
    protected var models: MutableList<Choice> = ArrayList()

    /**
     * The set of local values
     */
    @JvmField
    protected var local: PropertyMap = PropertyMap()

    /**
     * The set of property change listeners.
     */
    @JvmField
    protected var changeListeners: PropertyChangeSupport? = null

    /**
     * The list of listeners
     */
    @JvmField
    protected var listeners: MutableList<ConfigListener> = CopyOnWriteArrayList()

    companion object {
        /**
         * What is the Path of this key
         * 
         * @param key the key of the property
         * @return the path of the key
         */
        @JvmStatic
        fun getPath(key: String): String {
            val lastDot = key.lastIndexOf('.')
            if (lastDot == -1) {
                throw IllegalArgumentException("key=" + key + " does not contain a dot.")
            }

            return key.substring(0, lastDot)
        }

        /**
         * What is the last part of the Path of this key.
         * 
         * @param key the key of the property
         * @return the part of the path after the last dot, '.'
         */
        @JvmStatic
        fun getLeaf(key: String): String {
            val lastDot = key.lastIndexOf('.')
            if (lastDot == -1) {
                throw IllegalArgumentException("key=" + key + " does not contain a dot.")
            }

            return key.substring(lastDot + 1)
        }

        /**
         * The log stream
         */
        private val LOGGER = LoggerFactory.getLogger(Config::class.java)
    }
}
