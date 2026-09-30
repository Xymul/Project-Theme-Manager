package io.xymul.projtm.core

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

const val PLUGIN_ID = "io.xymul.projtm"

// Version of this plugin taken from its plugin descriptor.
fun pluginVersion(): String =
    runCatching { PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version }.getOrNull() ?: "0.0.0"

// Name of the running IDE, e.g. IntelliJ IDEA.
fun ideName(): String =
    runCatching { ApplicationInfo.getInstance().versionName }.getOrNull() ?: "IntelliJ Platform"

// Full version of the running IDE, e.g. 2026.2.3.
fun ideVersion(): String =
    runCatching { ApplicationInfo.getInstance().fullVersion }.getOrNull() ?: "0.0.0"
