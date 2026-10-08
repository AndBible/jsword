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
package org.crosswire.common.xml

import java.io.IOException
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import org.crosswire.common.util.ReflectionUtil
import org.xml.sax.SAXException
import org.xml.sax.XMLReader

/**
 * Runs an xml parser on an xml file using an xml handler. The default behavior
 * is to check that the xml file is well-formed.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class XMLProcess {
    /**
     * @return Returns the features.
     */
    open val features: XMLFeatureSet = XMLFeatureSet()

    private fun initialize(argv: Array<String>) {
        // process arguments
        var i = 0
        while (i < argv.size) {
            val arg = argv[i]
            if (arg[0] == '-') {
                val option = arg.substring(1)
                if ("p" == option) {
                    // get parser name
                    if (++i == argv.size) {
                        System.err.println("error: Missing argument to -p option.")
                    }
                    parserName = argv[i]

                    createParser()
                    i++
                    continue
                }
                if ("a" == option) {
                    // get parser name
                    if (++i == argv.size) {
                        System.err.println("error: Missing argument to -a option.")
                    }
                    adapterName = argv[i]

                    createAdapter()
                    i++
                    continue
                }
            }
            i++
        }

        features.setFeatureStates(argv)
    }

    private fun bind() {
        createParser()
        createAdapter()

        // Now that we have a parser and a handler
        // make the parser use them.
        setHandlers()
        features.setFeatures(parser!!)

    }

    private fun createParser() {
        if (parser != null) {
            return
        }

        try {
            parser = SAXParserFactory.newInstance().newSAXParser().xmlReader
//            parser = XMLReaderFactory.createXMLReader(parserName);
        } catch (e: SAXException) {
            System.err.println("error: Unable to instantiate parser")
        } catch (e: ParserConfigurationException) {
            System.err.println("error: Unable to instantiate parser")
        }

    }

    private fun createAdapter() {
        if (adapter != null) {
            return
        }

        try {
            adapter = ReflectionUtil.construct<XMLHandlerAdapter>(adapterName)
        } catch (e: ReflectiveOperationException) {
            System.err.println("error: Unable to instantiate XMLHandlerAdpater ($adapterName)")
        }

    }

    private fun setHandlers() {
        parser!!.setDTDHandler(adapter)
        parser!!.setErrorHandler(adapter)
        parser!!.setContentHandler(adapter)

        try {
            parser!!.setProperty(DECLARATION_HANDLER_PROPERTY_ID, adapter)
        } catch (e: SAXException) {
            e.printStackTrace(System.err)
        }

        try {
            parser!!.setProperty(LEXICAL_HANDLER_PROPERTY_ID, adapter)
        } catch (e: SAXException) {
            e.printStackTrace(System.err)
        }
    }

    /**
     * Parse an xml file.
     * 
     * @param xmlFile the file to parse
     */
    open fun parse(xmlFile: String?) {
        bind()
        // parse file
        try {
            System.out.println("Parsing with the following:")
            printActual()
            parser!!.parse(xmlFile)
            System.out.println("Done: no problems found.")
        } catch (e: SAXException) {
            System.err.println("error: Parse error occurred - " + e.message)
            val nested = e.exception
            if (nested != null) {
                nested.printStackTrace(System.err)
            } else {
                e.printStackTrace(System.err)
            }
        } catch (e: IOException) {
            e.printStackTrace(System.err)
        }
    }

    private fun usage() {
        System.err.println("usage: java org.crosswire.common.xml.XMLProcess (options) uri")
        System.err.println()

        System.err.println("options:")
        printUsage()
        System.err.println("  -h          This help screen.")
        System.err.println()

        System.err.println("defaults:")
        printDefaults()
    }

    /** Prints the usage.  */
    open fun printUsage() {
        System.err.println("  -p name     Select parser by name.")
        System.err.println("  -a name     Select XMLHandlerAdapter by name.")
        features.printUsage()
    }

    /**
     * Prints the default settings.
     */
    open fun printDefaults() {
        System.err.println("Parser:     " + DEFAULT_PARSER_NAME)
        System.err.println("Handler:    " + DEFAULT_HANDLER_NAME)
        System.err.println(XMLFeatureSet().toString())
    }

    /**
     * Prints the settings in effect.
     */
    open fun printActual() {
        System.err.println("Parser:     " + parserName)
        System.err.println("Handler:    " + adapterName)
        System.err.println(XMLFeatureSet().toString())
    }

    private var parserName: String = DEFAULT_PARSER_NAME
    private var parser: XMLReader? = null
    private var adapterName: String = DEFAULT_HANDLER_NAME
    private var adapter: XMLHandlerAdapter? = null

    companion object {
        /**
         * Process an xml file according to the arguments.
         * 
         * @param argv the command-line arguments
         */
        @JvmStatic
        fun main(argv: Array<String>) {
            val checker = XMLProcess()

            // is there anything to do?
            if (argv.size == 0) {
                checker.usage()
                System.exit(1)
            }

            // variables
            var arg: String? = null

            // process arguments
            for (i in argv.indices) {
                arg = argv[i]
                if (arg[0] == '-') {
                    val option = arg.substring(1)
                    if ("h" == option) {
                        checker.usage()
                        System.exit(0)
                    }
                }
            }

            checker.initialize(argv)
            checker.parse(arg)

        }

        // property ids

        /**
         * Lexical handler property id
         */
        private const val LEXICAL_HANDLER_PROPERTY_ID = "http://xml.org/sax/properties/lexical-handler"

        /**
         * Declaration handler property id
         */
        private const val DECLARATION_HANDLER_PROPERTY_ID = "http://xml.org/sax/properties/declaration-handler"

        // default settings

        /** Default parser name.  */
        private const val DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser"
        private const val DEFAULT_HANDLER_NAME = "org.crosswire.common.xml.XMLHandlerAdapter"
    }
}
