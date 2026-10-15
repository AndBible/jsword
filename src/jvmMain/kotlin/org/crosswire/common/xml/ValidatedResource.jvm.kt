package org.crosswire.common.xml

import fi.sykero.kdom.Document
import org.crosswire.common.util.ResourceUtil
import org.crosswire.common.util.LoggerFactory

internal actual fun loadValidatedResource(subject: String?): Document {
    // Explicit caller keeps XMLUtil-relative lookup without relying on stack depth across the seam.
    val input = ResourceUtil.getResourceAsStream(XMLUtil::class.java, subject + ".xml")
    LoggerFactory.getLogger(XMLUtil::class).debug("Loading {}.xml from classpath: [OK]", subject)
    return buildValidatedResource(input)
}
