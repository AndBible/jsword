package org.crosswire.jsword.book.filter.gbf

internal actual fun gbfDiagnosticClassName(value: Any): String? =
    org.crosswire.common.util.ClassUtil.getShortClassName(value.javaClass)

actual typealias GbfStack<E> = java.util.LinkedList<E>
