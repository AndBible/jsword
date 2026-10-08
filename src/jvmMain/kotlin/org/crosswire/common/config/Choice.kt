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
package org.crosswire.common.config

import java.util.ResourceBundle
import org.jdom2.Element

/**
 * Choice is the fundamental building block of the config system.
 * 
 * Every Choice must be able to:
 * <ul>
 * <li>get and set itself using a String</li>
 * <li>provide some simple help about itself</li>
 * <li>elect a user level for itself (Beginner, Intermediate, Advanced)</li>
 * <li>provide a GUI editor for itself</li>
 * </ul>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
interface Choice {
    /**
     * String value to associate with the name (key)
     * 
     * @param option the option
     * @param configResources the resource bundle to load
     * @throws StartupException if startup is not possible
     */
    @Throws(StartupException::class)
    fun init(option: Element, configResources: ResourceBundle)

    /**
     * The key of the option.
     * 
     * @return String The key string as supplied in config.xml
     */
    val key: String?

    /**
     * The full path of the option; set it to the path string as supplied in
     * config.properties.
     * 
     * @return String The path string as supplied in config.properties
     */
    var fullPath: String?

    /**
     * The type by which UIs can pick an appropriate editor
     * 
     * @return String The type string as supplied in config.xml
     */
    val type: String?

    /**
     * The class that this Choice works on. Used to decide how to display the
     * choice to the user.
     * 
     * @return The Class that this Choice works using.
     */
    val conversionClass: Class<*>

    /**
     * String value to associate with the name (key). Setting it can throw any
     * Exception since almost anything could go wrong at this point. The Config
     * dialog ought to cope with any errors.
     * 
     * @return value of this Choice
     * @throws ConfigException (when set) if there is a problem with the configuration
     */
    @set:Throws(ConfigException::class)
    var string: String?

    /**
     * A brief description of what is going on; set it to some help text.
     * 
     * @return Some help text
     */
    var helpText: String?

    /**
     * Is this Choice OK to write out to a file, or should we use settings in
     * this run of the program, but forget them for next time. A typical use of
     * this is for password configuration.
     * 
     * @return True if it is safe to store the value in a config file.
     */
    val isSaveable: Boolean

    /**
     * Whether this should be visible in a Config Editor.
     * 
     * @return hidden or visible
     */
    val isHidden: Boolean

    /**
     * Whether this should be ignored altogether in a Config Editor.
     * 
     * @return hidden or visible
     */
    val isIgnored: Boolean

    /**
     * Do we need to restart the program in order for this change to have
     * effect?
     * 
     * @return True if a restart is required
     */
    fun requiresRestart(): Boolean
}
