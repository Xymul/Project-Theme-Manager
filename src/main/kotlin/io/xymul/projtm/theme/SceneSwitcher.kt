package io.xymul.projtm.theme

import com.intellij.openapi.project.Project
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.projectPathOf
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.Scene

// Reads the work scenes of a project and applies one of them.
object SceneSwitcher {

    fun scenesOf(project: Project): List<Scene> = scenesConfig(project)?.scenes.orEmpty()

    fun activeScene(project: Project): String? = ActiveSceneService.getInstance(project).activeScene

    // Switches the theme and remembers the scene as the active one.
    fun switchTo(project: Project, sceneName: String): Boolean {
        val scene = scenesConfig(project)?.scenes?.firstOrNull { it.name == sceneName } ?: return false
        if (!ThemeService.getInstance().applyTheme(scene.themeId)) return false
        ActiveSceneService.getInstance(project).markApplied(scene.name)
        return true
    }

    private fun scenesConfig(project: Project): ProjectSceneConfig? {
        val path = projectPathOf(project) ?: return null
        val repository = ConfigRepository.getInstance()
        val entry = repository.findProject(path) ?: return null
        return repository.scenesOf(entry)
    }
}
