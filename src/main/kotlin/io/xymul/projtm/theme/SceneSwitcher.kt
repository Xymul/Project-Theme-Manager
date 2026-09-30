package io.xymul.projtm.theme

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.NOTIFICATION_GROUP_ID
import io.xymul.projtm.core.projectPathOf
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.Scene
import io.xymul.projtm.ui.ProjectThemeManagerBundle

// Reads the work scenes of a project and applies one of them.
object SceneSwitcher {

    fun scenesOf(project: Project): List<Scene> = scenesConfig(project)?.scenes.orEmpty()

    fun activeScene(project: Project): String? = ActiveSceneService.getInstance(project).activeScene

    fun switchToDefault(project: Project): Boolean =
        scenesConfig(project)?.defaultScene?.let { switchTo(project, it) } ?: false

    // Switches to the scene and remembers it, reports a missing theme instead of failing silently.
    fun switchTo(project: Project, sceneName: String): Boolean {
        val scene = scenesConfig(project)?.scenes?.firstOrNull { it.name == sceneName } ?: return false
        if (!ThemeService.getInstance().applyThemeByName(scene.theme)) {
            notifyMissingTheme(scene)
            return false
        }
        ActiveSceneService.getInstance(project).markApplied(scene.name)
        return true
    }

    private fun notifyMissingTheme(scene: Scene) {
        if (ApplicationManager.getApplication() == null) return
        runCatching {
            NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(
                    ProjectThemeManagerBundle.message("notification.title"),
                    ProjectThemeManagerBundle.message("theme.notInstalled", scene.theme),
                    NotificationType.WARNING,
                )
                .notify(null)
        }
    }

    private fun scenesConfig(project: Project): ProjectSceneConfig? {
        val path = projectPathOf(project) ?: return null
        val repository = ConfigRepository.getInstance()
        val entry = repository.findProject(path) ?: return null
        return repository.scenesOf(entry)
    }
}
