package io.xymul.projtm.ui

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_NAME = "messages.ProjectThemeManagerBundle"

object ProjectThemeManagerBundle : DynamicBundle(BUNDLE_NAME) {

    fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
