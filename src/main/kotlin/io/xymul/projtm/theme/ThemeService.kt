package io.xymul.projtm.theme

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

data class ThemeItem(val id: String, val name: String, val dark: Boolean)

// Reads, switches and observes the user interface themes of the IDE.
@Service(Service.Level.APP)
class ThemeService {

    companion object {
        fun getInstance(): ThemeService = ApplicationManager.getApplication().getService(ThemeService::class.java)
    }

    fun themes(): List<ThemeItem> {
        val manager = lafManager() ?: return emptyList()
        return runCatching { manager.installedThemes }
            .getOrNull()?.map { ThemeItem(it.id, it.name, it.isDark) }
            ?.toList()
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    fun currentThemeId(): String? =
        runCatching { lafManager()?.currentUIThemeLookAndFeel?.id }.getOrNull()

    fun themeName(themeId: String): String? = themes().firstOrNull { it.id == themeId }?.name

    fun isDark(themeId: String): Boolean = themes().firstOrNull { it.id == themeId }?.dark ?: false

    // Switches the theme on the EDT, returns false when the theme is not installed.
    fun applyTheme(themeId: String): Boolean {
        val manager = lafManager() ?: return false
        val theme = runCatching { manager.findLaf(themeId) }.getOrNull() ?: return false
        val application = ApplicationManager.getApplication() ?: return false
        application.invokeLater { manager.setCurrentUIThemeLookAndFeel(theme) }
        return true
    }

    fun subscribe(parentDisposable: Disposable, listener: () -> Unit) {
        val application = ApplicationManager.getApplication() ?: return
        application.messageBus.connect(parentDisposable)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener { listener() })
    }

    private fun lafManager(): LafManager? = runCatching { LafManager.getInstance() }.getOrNull()
}
