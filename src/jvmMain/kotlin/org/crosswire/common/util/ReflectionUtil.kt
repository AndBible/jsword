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

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Various utilities for calling constructors and methods via introspection.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class ReflectionUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Build an object using its default constructor. Note: a constructor that
         * takes a boolean needs a type of boolean.class, but a parameter of type
         * Boolean. Likewise for other primitives. If this is needed, do not call
         * this method.
         *
         * @param <T> the type of the object to construct
         * @param className
         *            the full class name of the object
         * @return the constructed object
         * @throws ReflectiveOperationException
         *               if this {@code className} represents an abstract class,
         *               an interface, an array class, a primitive type, or void;
         *               or if the class has no reachable nullary constructor;
         *               or if the instantiation fails for some other reason.
         */
        @JvmStatic
        @Throws(ReflectiveOperationException::class)
        @Suppress("UNCHECKED_CAST")
        fun <T> construct(className: String?): T {
            return construct(ClassUtil.forName(className) as Class<T>)
        }

        /**
         * Build an object using its default constructor. Note: a constructor that
         * takes a boolean needs a type of boolean.class, but a parameter of type
         * Boolean. Likewise for other primitives. If this is needed, do not call
         * this method.
         *
         * @param <T> the type of the object to construct
         * @param clazz
         *            the class of the object
         * @return the constructed object
         * @throws ReflectiveOperationException
         *               if this {@code clazz} represents an abstract class,
         *               an interface, an array class, a primitive type, or void;
         *               or if the class has no reachable nullary constructor;
         *               or if the instantiation fails for some other reason.
         */
        @JvmStatic
        @Throws(ReflectiveOperationException::class)
        fun <T> construct(clazz: Class<T>?): T {
            return clazz!!.getDeclaredConstructor().newInstance()
        }

        /**
         * Build an object using the supplied parameters. Note: a constructor that
         * takes a boolean needs a type of boolean.class, but a parameter of type
         * Boolean. Likewise for other primitives.
         *
         * @param <T> the type of the object to construct
         * @param className
         *            the full class name of the object
         * @param params
         *            the constructor's arguments
         * @return the built object
         * @throws ClassNotFoundException if the class is not found
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws  InstantiationException
         *               if this {@code data} represents an abstract class,
         *               an interface, an array class, a primitive type, or void;
         *               or if the class has no nullary constructor;
         *               or if the instantiation fails for some other reason.
         * @throws IllegalAccessException  if the class or its nullary
         *               constructor is not accessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         * @throws InstantiationException
         *              if the class that declares the
         *              underlying constructor represents an abstract class.
         */
        @JvmStatic
        @Throws(
            ClassNotFoundException::class,
            NoSuchMethodException::class,
            IllegalAccessException::class,
            InvocationTargetException::class,
            InstantiationException::class
        )
        @Suppress("UNCHECKED_CAST")
        fun <T> construct(className: String?, vararg params: Any?): T {
            val clazz = ClassUtil.forName(className) as Class<T>
            return construct(clazz, *params)
        }

        /**
         * Build an object using the supplied parameters. Note: a constructor that
         * takes a boolean needs a type of boolean.class, but a parameter of type
         * Boolean. Likewise for other primitives.
         *
         * @param <T> the type of the object to construct
         * @param clazz
         *            the class of the object
         * @param params
         *            the constructor's arguments
         * @return the built object
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws InstantiationException
         *              if the class that declares the
         *              underlying constructor represents an abstract class.
         * @throws IllegalAccessException
         *              if this {@code Constructor} object
         *              is enforcing Java language access control and the underlying
         *              constructor is inaccessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         */
        @JvmStatic
        @Throws(
            NoSuchMethodException::class,
            InstantiationException::class,
            IllegalAccessException::class,
            InvocationTargetException::class
        )
        fun <T> construct(clazz: Class<T>?, vararg params: Any?): T {
            val paramTypes = describeParameters(*params)
            val c = clazz!!.getConstructor(*paramTypes)
            return c.newInstance(*params)
        }

        /**
         * Build an object using the supplied parameters.
         *
         * @param <T> the type of the object to construct
         * @param className
         *            the full class name of the object
         * @param params
         *            the constructor's arguments
         * @param paramTypes
         *            the types of the parameters
         * @return the built object
         * @throws ClassNotFoundException if the class is not found
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws InstantiationException
         *              if the class that declares the
         *              underlying constructor represents an abstract class.
         * @throws IllegalAccessException
         *              if this {@code Constructor} object
         *              is enforcing Java language access control and the underlying
         *              constructor is inaccessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         */
        @JvmStatic
        @Throws(
            ClassNotFoundException::class,
            NoSuchMethodException::class,
            IllegalAccessException::class,
            InvocationTargetException::class,
            InstantiationException::class
        )
        @Suppress("UNCHECKED_CAST")
        fun <T> construct(className: String?, params: Array<Any?>?, paramTypes: Array<Class<*>?>?): T {
            var calledTypes = paramTypes
            if (calledTypes == null) {
                calledTypes = describeParameters(*params!!)
            }
            val clazz = ClassUtil.forName(className) as Class<T>
            val c = clazz.getConstructor(*calledTypes)
            // A null array means "no arguments" to Constructor.newInstance, as in Java
            return if (params == null) c.newInstance() else c.newInstance(*params)
        }

        /**
         * Call a method on a class given a sting
         *
         * @param base
         *            The object to invoke a method on
         * @param methodName
         *            The text of the invocation, for example "getName"
         * @param params
         *            For example new Object[] { ...}
         * @return whatever the method returs
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws IllegalAccessException
         *              if this {@code Constructor} object
         *              is enforcing Java language access control and the underlying
         *              constructor is inaccessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         */
        @JvmStatic
        @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
        @Suppress("UNCHECKED_CAST")
        fun invoke(base: Any?, methodName: String?, vararg params: Any?): Any? {
            val clazz: Class<*> = base!!.javaClass
            return invoke(clazz, base, methodName, params as Array<Any?>, null)
        }

        /**
         * Call a static method on a class given a string
         *
         * @param call
         *            The text of the invocation, for example
         *            "java.lang.String.getName"
         * @param params
         *            For example new Object[] { ...}
         * @return whatever the method returs
         * @throws ClassNotFoundException if the class is not found
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws IllegalAccessException
         *              if this {@code Constructor} object
         *              is enforcing Java language access control and the underlying
         *              constructor is inaccessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         */
        @JvmStatic
        @Throws(
            ClassNotFoundException::class,
            NoSuchMethodException::class,
            IllegalAccessException::class,
            InvocationTargetException::class
        )
        @Suppress("UNCHECKED_CAST")
        fun invoke(call: String, vararg params: Any?): Any? {
            // Split the call into class name and method name
            val lastDot = call.lastIndexOf('.')
            val className = call.substring(0, lastDot)
            val methodName = call.substring(lastDot + 1)
            val clazz = ClassUtil.forName(className)
            return invoke(clazz, clazz, methodName, params as Array<Any?>, null)
        }

        /**
         * Call a method on an object, or statically, with the supplied parameters.
         *
         * Note: a method that takes a boolean needs a type of boolean.class, but a
         * parameter of type Boolean. Likewise for other primitives. If this is
         * needed, do not call this method.
         *
         * @param <T> the type of the object to construct
         * @param clazz
         *            the class of the object
         * @param obj
         *            the object having the method, or null to call a static method
         * @param methodName
         *            the method to be called
         * @param params
         *            the parameters
         * @return whatever the method returns
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws IllegalAccessException
         *              if this {@code Constructor} object
         *              is enforcing Java language access control and the underlying
         *              constructor is inaccessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         */
        @JvmStatic
        @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
        @Suppress("UNCHECKED_CAST")
        fun <T> invoke(clazz: Class<T>?, obj: Any?, methodName: String?, vararg params: Any?): Any? {
            return invoke(clazz, obj, methodName, params as Array<Any?>, null)
        }

        /**
         * Call a method on an object, or statically, with the supplied parameters.
         *
         * Note: a method that takes a boolean needs a type of boolean.class, but a
         * parameter of type Boolean. Likewise for other primitives.
         *
         * @param <T> the type of the object to construct
         * @param clazz
         *            the class of the object
         * @param obj
         *            the object having the method, or null to call a static method
         * @param methodName
         *            the method to be called
         * @param params
         *            the parameters
         * @param paramTypes
         *            the types of each of the parameters
         * @return whatever the method returns
         * @throws NoSuchMethodException
         *              the method does not exist
         * @throws IllegalAccessException
         *              if this {@code Constructor} object
         *              is enforcing Java language access control and the underlying
         *              constructor is inaccessible.
         * @throws InvocationTargetException if the underlying constructor
         *              throws an exception.
         */
        @JvmStatic
        @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
        fun <T> invoke(clazz: Class<T>?, obj: Any?, methodName: String?, params: Array<Any?>?, paramTypes: Array<Class<*>?>?): Any? {
            var calledTypes = paramTypes
            if (calledTypes == null) {
                calledTypes = describeParameters(*params!!)
            }
            val method = getMethod(clazz, methodName, calledTypes)
            // A null array means "no arguments" to Method.invoke, as in Java
            return if (params == null) method.invoke(obj) else method.invoke(obj, *params)
        }

        /**
         * Construct a parallel array of class objects for each element in params.
         *
         * @param params
         *            the types to describe
         * @return the parallel array of class objects
         */
        private fun describeParameters(vararg params: Any?): Array<Class<*>?> {
            val calledTypes = arrayOfNulls<Class<*>>(params.size)
            for (i in params.indices) {
                var clazz: Class<*> = params[i]!!.javaClass
                if (clazz == Boolean::class.javaObjectType) {
                    clazz = java.lang.Boolean.TYPE
                }
                calledTypes[i] = clazz
            }
            return calledTypes
        }

        @Throws(NoSuchMethodException::class)
        private fun <T> getMethod(clazz: Class<T>?, methodName: String?, calledTypes: Array<Class<*>?>): Method {
            // The bad news is that we can't use something like:
            // clazz.getMethod(methodNames, called_types);
            // because it does not cope with inheritance (at least in the MVM)
            // so we have to search ourselves...
            val testMethods = clazz!!.methods
            outer@ for (i in testMethods.indices) {
                // This this the right method name?
                if (testMethods[i].name != methodName) {
                    continue@outer
                }

                // The right number of params
                val testTypes = testMethods[i].parameterTypes
                if (testTypes.size != calledTypes.size) {
                    continue
                }

                // Of the right types?
                for (j in testTypes.indices) {
                    if (!testTypes[j].isAssignableFrom(calledTypes[j])) {
                        continue@outer
                    }
                }

                // So this is a match
                return testMethods[i]
            }

            throw NoSuchMethodException(methodName)
        }
    }
}
