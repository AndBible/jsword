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

import java.util.concurrent.CopyOnWriteArrayList

import org.crosswire.jsword.JSMsg

/**
 * This package looks after Exceptions and messages as they happen. It would be
 * nice not to need this class - the principle being that any library that
 * encounters an error can throw an exception to indicate that there is a
 * problem. However this is not always the case. For example:
 *
 *  * static class constructors should not throw, unless the class really is of
 * no use given the error, and yet we may want to tell the user that there was a
 * (non-critical) error.
 *  * Any library routine that works in a loop, applying some (potentially
 * failing) functionality, may want to continue the work without throwing in
 * response to a single error.
 *  * The class being implemented may implement an interface that disallows
 * nested exceptions and yet does not want to loose the root cause error
 * information. (This is the weakest of the above arguments, but probably still
 * valid.)
 *
 * However in many of the times this class is used, this is the
 * reason:
 *
 *  * Within UI specific code - to throw up a dialog box (or whatever). Now
 * this use is currently tolerated, however it is probably a poor idea to use
 * GUI agnostic messaging in a GUI specific context. But I'm not bothered enough
 * to change it now. Specifically this use is deprecated because it makes the
 * app more susceptible to the configuration of the things that listen to
 * reports.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class Reporter
/**
 * Enforce Singleton
 */
private constructor() {
    companion object {
        /**
         * Something has gone wrong. We need to tell the user or someone, but we can
         * carry on. In general having caught an exception and passed it to
         * Reporter.informUser(), you should not throw another Exception. Called to
         * fire a commandEntered event to all the Listeners
         *
         * @param source
         *            The cause of the problem, a Component if possible.
         * @param prob
         *            The Exception that was thrown
         */
        @JvmStatic
        fun informUser(source: Any?, prob: Throwable) {
            val cat: Class<*> = if (source != null) source.javaClass else Reporter::class.java
            val templog = LoggerFactory.getLogger(cat.kotlin)

            templog.warn(prob.message, prob)

            fireCapture(ReporterEvent(source, prob))
        }

        /**
         * Something has gone wrong. We need to tell the user or someone, but we can
         * carry on. In general having caught an exception and passed it to
         * Reporter.informUser(), you should not throw another Exception. Called to
         * fire a commandEntered event to all the Listeners
         *
         * @param source
         *            The cause of the problem, a Component if possible.
         * @param prob
         *            The Exception that was thrown
         */
        @JvmStatic
        fun informUser(source: Any?, prob: LucidException) {
            val cat: Class<*> = if (source != null) source.javaClass else Reporter::class.java
            val templog = LoggerFactory.getLogger(cat.kotlin)

            templog.warn(prob.message, prob)

            fireCapture(ReporterEvent(source, prob))
        }

        /**
         * Something has gone wrong. We need to tell the user or someone, but we can
         * carry on. In general having caught an exception and passed it to
         * Reporter.informUser(), you should not throw another Exception. Called to
         * fire a commandEntered event to all the Listeners
         *
         * @param source
         *            The cause of the problem, a Component if possible.
         * @param prob
         *            The Exception that was thrown
         */
        @JvmStatic
        fun informUser(source: Any?, prob: LucidRuntimeException) {
            val cat: Class<*> = if (source != null) source.javaClass else Reporter::class.java
            val templog = LoggerFactory.getLogger(cat.kotlin)

            templog.warn(prob.message, prob)

            fireCapture(ReporterEvent(source, prob))
        }

        /**
         * Something has happened. We need to tell the user or someone.
         *
         * @param source
         *            The cause of the message, a Component if possible.
         * @param message
         *            The message to pass to the user
         */
        @JvmStatic
        fun informUser(source: Any?, message: String?) {
            LOGGER.debug(message)

            fireCapture(ReporterEvent(source, message))
        }

        /**
         * Add an Exception listener to the list of things wanting to know whenever
         * we capture an Exception
         *
         * @param li the interested listener
         */
        @JvmStatic
        fun addReporterListener(li: ReporterListener) {
            LISTENERS.add(li)
        }

        /**
         * Remove an Exception listener from the list of things wanting to know
         * whenever we capture an Exception
         *
         * @param li the disinterested listener
         */
        @JvmStatic
        fun removeReporterListener(li: ReporterListener) {
            LISTENERS.remove(li)
        }

        /**
         * Log a message.
         *
         * @param ev the event to capture
         */
        @JvmStatic
        protected fun fireCapture(ev: ReporterEvent) {
            if (LISTENERS.size == 0) {
                LOGGER.warn("Nothing to listen to report: message={}", ev.message, ev.exception)
            }

            for (listener in LISTENERS) {
                if (ev.exception != null) {
                    listener.reportException(ev)
                } else {
                    listener.reportMessage(ev)
                }
            }
        }

        /**
         * Sets the parent of any exception windows.
         *
         * @param grab whether to grab AWT exceptions or not
         */
        @JvmStatic
        fun grabAWTExecptions(grab: Boolean) {
            if (grab) {
                // register ourselves
                System.setProperty(AWT_HANDLER_PROPERTY, OUR_NAME)
            } else {
                // unregister ourselves
                val current = System.getProperty(AWT_HANDLER_PROPERTY)
                if (current != null && current == OUR_NAME) {
                    val prop = System.getProperties()
                    prop.remove(AWT_HANDLER_PROPERTY)
                }
            }
        }

        /**
         * The system property name for registering AWT exceptions
         */
        private const val AWT_HANDLER_PROPERTY = "sun.awt.exception.handler"

        /**
         * The name of the class to register for AWT exceptions
         */
        private val OUR_NAME: String = CustomAWTExceptionHandler::class.java.name

        /**
         * The list of listeners
         */
        private val LISTENERS: MutableList<ReporterListener> = CopyOnWriteArrayList()

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(Reporter::class)
    }

    /**
     * A class to handle AWT caught Exceptions
     */
    class CustomAWTExceptionHandler {
        /**
         * Its important that we have a no-arg ctor to make this work. So if we
         * ever create an arged ctor then we need to add:
         * public CustomAWTExceptionHandler() { }
         */

        /**
         * Handle AWT exceptions
         *
         * @param ex the exception to handle
         */
        fun handle(ex: Throwable?) {
            // Only allow one to be reported every so often.
            // This interval control was needed because AWT exceptions
            // were causing recursive AWT exceptions
            // and way too many dialogs were being thrown up on the screen.
            if (gate.open()) {
                // TRANSLATOR: Very frequent error condition: The program has encountered a severe problem and it is likely that the program is unusable.
                informUser(this, LucidException(JSMsg.gettext("Unexpected internal problem. You may need to restart."), ex))
            }
        }

        private companion object {
            private val gate = TimeGate(2000)
        }
    }
}
