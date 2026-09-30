package io.xymul.projtm.theme

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

// Themes are identified by their display name.
data class ThemeItem(val name: String)

// Reads, switches and observes the user interface themes of the IDE.
@Service(Service.Level.APP)
class ThemeService {

    companion object {
        fun getInstance(): ThemeService = ApplicationManager.getApplication().getService(ThemeService::class.java)
    }

    fun themes(): List<ThemeItem> {
        val manager = lafManager() ?: return emptyList()
        return runCatching { manager.installedThemes }
            .getOrNull()?.map { ThemeItem(it.name) }
            ?.toList()
            ?.distinctBy { it.name }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    fun currentThemeName(): String? = runCatching { lafManager()?.currentUIThemeLookAndFeel?.name }.getOrNull()

    fun isInstalled(themeName: String): Boolean = themes().any { it.name == themeName }

    // Switches the theme by name, returns false when no theme with that name is installed.
    fun applyThemeByName(themeName: String): Boolean {
        val manager = lafManager() ?: return false
        val theme = runCatching { manager.installedThemes.firstOrNull { it.name == themeName } }.getOrNull()
            ?: return false
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
