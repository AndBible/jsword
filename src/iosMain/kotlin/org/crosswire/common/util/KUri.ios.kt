package org.crosswire.common.util

actual class KUri private constructor(private val model: KUriModel) {
    actual constructor(str: String) : this(KUriModel.parse(str))
    actual constructor(scheme: String?, userInfo: String?, host: String?, port: Int, path: String?, query: String?, fragment: String?) : this(KUriModel.build(scheme, userInfo, host, port, path, query, fragment))
    actual constructor(scheme: String?, host: String?, path: String?, fragment: String?) : this(KUriModel.build(scheme, host, path, fragment))
    internal fun rawAuthority(): String? = model.rawAuthority()
    actual fun getScheme(): String? = model.getScheme()
    actual fun getUserInfo(): String? = model.getUserInfo()
    actual fun getHost(): String? = model.getHost()
    actual fun getPort(): Int = model.getPort()
    actual fun getPath(): String? = model.getPath()
    actual fun getRawPath(): String? = model.getRawPath()
    actual fun getQuery(): String? = model.getQuery()
    actual fun getFragment(): String? = model.getFragment()
    actual fun isAbsolute(): Boolean = model.isAbsolute()
    actual override fun toString(): String = model.toString()
    actual override fun equals(other: Any?): Boolean = other is KUri && model == other.model
    actual override fun hashCode(): Int = model.hashCode()
}
