package org.crosswire.common.util

import kotlin.reflect.KClass

internal actual fun assertionsEnabled(type: KClass<*>): Boolean = false
