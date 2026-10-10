package org.crosswire.common.util

import org.crosswire.jsword.internationalisation.LocaleProviderManager
import java.util.Locale

/** Keeps language, country and variant only; script and `-u-` extensions are dropped. */
fun Locale.toKLocale(): KLocale = KLocale(language, country, variant)
fun KLocale.toJavaLocale(): Locale = Locale(language, country, variant)

internal actual fun platformCurrentLocale(): KLocale = LocaleProviderManager.getLocale()
internal actual fun platformDefaultLocale(): KLocale = Locale.getDefault().toKLocale()
