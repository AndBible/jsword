package org.crosswire.common.util

import kotlin.reflect.KClass

/** Keeps class-specific JVM assertion checks lazy; Native assertions are disabled. */
internal expect fun assertionsEnabled(type: KClass<*>): Boolean
