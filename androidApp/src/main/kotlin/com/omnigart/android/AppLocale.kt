package com.omnigart.android

import com.omnigart.shared.i18n.OmniLocales
import java.util.Locale

/** Device locale resolved to a shipped OmniGart catalog tag. */
internal fun appLocaleTag(): String = OmniLocales.resolve(Locale.getDefault().toLanguageTag())
