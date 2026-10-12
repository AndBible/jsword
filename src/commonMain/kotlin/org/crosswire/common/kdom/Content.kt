package org.crosswire.common.kdom

/**
 * K4 seam (spec §4.2.1): the node type JSword's backends pass through without looking inside.
 * JVM: `org.jdom2.Content`. iOS: opaque until K5 replaces this with the real kdom class.
 * jdom2 declares equals/hashCode final; the expect must say so or the typealias is rejected.
 */
expect abstract class Content {
    final override fun equals(other: Any?): Boolean
    final override fun hashCode(): Int
}

/** K4 seam: JVM `org.jdom2.Document`, iOS opaque until K5. */
expect open class Document {
    final override fun equals(other: Any?): Boolean
    final override fun hashCode(): Int
}
