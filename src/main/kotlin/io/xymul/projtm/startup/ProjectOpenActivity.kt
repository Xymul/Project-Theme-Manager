package io.xymul.projtm.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.theme.ActiveSceneService
import io.xymul.projtm.theme.ThemeService
import io.xymul.projtm.ui.projectPathOf

// Applies the default work scene of a project right after it was opened.
class ProjectOpenActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val path = projectPathOf(project) ?: return
        val repository = ConfigRepository.getInstance()
        val entry = repository.findProject(path) ?: return
        val state = ActiveSceneService.getInstance(project)
        if (!state.applied) applyDefaultScene(project, entry.uuid)
        subscribeToThemeChanges(project, entry.uuid)
    }

    private fun applyDefaultScene(project: Project, uuid: String) {
        val repository = ConfigRepository.getInstance()
        val entry = repository.projects().firstOrNull { it.uuid == uuid } ?: return
        val config = repository.scenesOf(entry)
        val scene = config.scenes.firstOrNull { it.name == config.defaultScene } ?: return
        if (ThemeService.getInstance().applyTheme(scene.themeId)) {
            ActiveSceneService.getInstance(project).markApplied(scene.name)
        }
    }

    // A theme that no longer matches the active scene means the user changed it by hand.
    private fun subscribeToThemeChanges(project: Project, uuid: String) {
        ThemeService.getInstance().subscribe(project) {
            val current = ThemeService.getInstance().currentThemeId() ?: return@subscribe
            val repository = ConfigRepository.getInstance()
            val entry = repository.projects().firstOrNull { it.uuid == uuid } ?: return@subscribe
            val state = ActiveSceneService.getInstance(project)
            val activeName = state.activeScene ?: return@subscribe
            val sceneTheme = repository.scenesOf(entry).scenes.firstOrNull { it.name == activeName }?.themeId
            if (sceneTheme != null && sceneTheme != current) state.markDetached()
        }
    }
}
