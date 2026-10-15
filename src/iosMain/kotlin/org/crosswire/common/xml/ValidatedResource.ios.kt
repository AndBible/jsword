package org.crosswire.common.xml

import fi.sykero.kdom.Document

internal actual fun loadValidatedResource(subject: String?): Document =
    throw UnsupportedOperationException("Validating XML classpath resources are unavailable on Native")
