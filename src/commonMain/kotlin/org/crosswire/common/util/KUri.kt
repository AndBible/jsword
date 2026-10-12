package org.crosswire.common.util

/**
 * K4 URI seam: JVM uses java.net.URI unchanged; native behavior is pinned by differential tests.
 * Common callers use explicit getters rather than Java synthetic properties.
 */
expect class KUri {
    constructor(str: String)
    constructor(scheme: String?, userInfo: String?, host: String?, port: Int, path: String?, query: String?, fragment: String?)
    constructor(scheme: String?, host: String?, path: String?, fragment: String?)
    fun getScheme(): String?
    fun getUserInfo(): String?
    fun getHost(): String?
    fun getPort(): Int
    fun getPath(): String?
    fun getRawPath(): String?
    fun getQuery(): String?
    fun getFragment(): String?
    fun isAbsolute(): Boolean
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
