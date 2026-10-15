package org.crosswire.common.xml

import fi.sykero.kdom.Document

/** Platform resource lookup and DTD validation; not supported on Native. */
internal expect fun loadValidatedResource(subject: String?): Document
