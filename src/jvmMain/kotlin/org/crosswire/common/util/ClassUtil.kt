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
package org.crosswire.common.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Various Java Class Utilities.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class ClassUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Gets the Class for the className in a way that works well for extensions.
         * See: http://www.javageeks.com/Papers/ClassForName/ClassForName.pdf
         * 
         * @param className
         *            the class to get
         * @return the found Class
         * @throws ClassNotFoundException if the class is not found
         */
        @JvmStatic
        @Throws(ClassNotFoundException::class)
        fun forName(className: String?): Class<*> {
            return Thread.currentThread().contextClassLoader.loadClass(className)
        }

        /**
         * This function finds the first matching filename for a Java class file
         * from the classpath, if none is found it returns null.
         * 
         * @param className
         *            the class to get
         * @param classPath the lookup class path
         * @return the filename for the class
         */
        @JvmStatic
        fun findClasspathEntry(className: String, classPath: String?): String? {
            var full: String? = null

            val paths = StringUtil.split(classPath, File.pathSeparator)
            for (i in paths.indices) {
                // Search the jar
                if (paths[i].endsWith(EXTENSION_ZIP) || paths[i].endsWith(EXTENSION_JAR)) {
                    var zip: ZipFile? = null
                    try {
                        val fileName = className.replace(',', '/') + EXTENSION_CLASS
                        zip = ZipFile(paths[i])
                        val entry = zip.getEntry(fileName)

                        if (entry != null && !entry.isDirectory) {
                            if (full != null && full != fileName) {
                                LOGGER.warn("Warning duplicate {} found: {} and {}", className, full, paths[i])
                            } else {
                                full = paths[i]
                            }
                        }
                    } catch (ex: IOException) {
                        // If that zip file failed, then ignore it and move on.
                        LOGGER.warn("Missing zip file for {} and {}", className, paths[i])
                    } finally {
                        if (null != zip) {
                            try {
                                zip.close()
                            } catch (ex: IOException) {
                                LOGGER.error("close", ex)
                            }
                        }
                    }
                } else {
                    val path = StringBuilder(256)

                    // Search for the file
                    val extra = className.replace('.', File.separatorChar)

                    path.append(paths[i])
                    if (paths[i][paths[i].length - 1] != File.separatorChar) {
                        path.append(File.separatorChar)
                    }

                    path.append(extra)
                    path.append(EXTENSION_CLASS)
                    val fileName = path.toString()

                    if (File(fileName).isFile) {
                        if (full != null && full != fileName) {
                            LOGGER.warn("Warning duplicate {} found: {} and {}", className, full, paths[i])
                        } else {
                            full = paths[i]
                        }
                    }
                }
            }

            return full
        }

        /**
         * This function find the first matching filename for a Java class file from
         * the classpath, if none is found it returns null.
         * 
         * @param className
         *            the class to get
         * @return the filename for the class
         */
        @JvmStatic
        fun findClasspathEntry(className: String): String? {
            val classpath = System.getProperty("java.class.path", "")
            return findClasspathEntry(className, classpath)
        }

        /**
         * Gets the class name minus the package name for an <code>Object</code>.
         * 
         * @param object
         *            the class to get the short name for, may be null
         * @param valueIfNull
         *            the value to return if null
         * @return the class name of the object without the package name, or the
         *         null value
         */
        @JvmStatic
        fun getShortClassName(`object`: Any?, valueIfNull: String?): String? {
            if (`object` == null) {
                return valueIfNull
            }
            return getShortClassName(`object`.javaClass.name)
        }

        /**
         * Gets the class name minus the package name from a <code>Class</code>.
         * 
         * @param cls
         *            the class to get the short name for, must not be
         *            <code>null</code>
         * @return the class name without the package name
         * @throws IllegalArgumentException
         *             if the class is <code>null</code>
         */
        @JvmStatic
        fun getShortClassName(cls: Class<*>?): String {
            if (cls == null) {
                throw IllegalArgumentException("The class must not be null")
            }
            return getShortClassName(cls.name)
        }

        /**
         * Gets the class name minus the package name from a String.
         * 
         * <p>
         * The string passed in is assumed to be a class name - it is not checked.
         * </p>
         * 
         * @param className
         *            the className to get the short name for, must not be empty or
         *            <code>null</code>
         * @return the class name of the class without the package name
         * @throws IllegalArgumentException
         *             if the className is empty
         */
        @JvmStatic
        fun getShortClassName(className: String?): String {
            if (className == null || className.length == 0) {
                throw IllegalArgumentException("The class name must not be empty")
            }
            val chars = className.toCharArray()
            var lastDot = 0
            for (i in chars.indices) {
                if (chars[i] == PACKAGE_SEPARATOR_CHAR) {
                    lastDot = i + 1
                } else if (chars[i] == INNER_CLASS_SEPARATOR_CHAR) {
                    chars[i] = PACKAGE_SEPARATOR_CHAR
                }
            }
            return String(chars, lastDot, chars.size - lastDot)
        }

        /**
         * The package separator character: <code>&#x2e;</code>.
         */
        private const val PACKAGE_SEPARATOR_CHAR = '.'

        /**
         * The inner class separator character: <code>$</code>.
         */
        private const val INNER_CLASS_SEPARATOR_CHAR = '$'

        private const val EXTENSION_CLASS = ".class"
        private const val EXTENSION_JAR = ".jar"
        private const val EXTENSION_ZIP = ".zip"

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(ClassUtil::class.java)
    }
}
