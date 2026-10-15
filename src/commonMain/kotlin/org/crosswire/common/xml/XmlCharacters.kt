package org.crosswire.common.xml

/** Retain the JVM String constructor bounds and exception contract for SAX callbacks. */
internal expect fun xmlCharacters(chars: CharArray, offset: Int, length: Int): String

/** Keep Java checked callback exceptions without imposing an incompatible Native @Throws filter. */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION)
internal expect annotation class JvmCallbackThrows(vararg val exceptionClasses: kotlin.reflect.KClass<out Throwable>)
