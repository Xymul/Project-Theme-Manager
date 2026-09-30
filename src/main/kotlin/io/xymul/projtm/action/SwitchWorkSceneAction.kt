package io.xymul.projtm.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import io.xymul.projtm.theme.SceneSwitcher
import io.xymul.projtm.ui.ProjectThemeManagerBundle
import io.xymul.projtm.ui.showSceneChooser

// Quick switch between the work scenes of the current project.
class SwitchWorkSceneAction : AnAction(), DumbAware {

    // The data context of the menu is only reliable on the EDT.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = choicesOf(event.project).isNotEmpty()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val choices = choicesOf(project)
        if (choices.isEmpty()) return
        showSceneChooser(
            choices,
            ProjectThemeManagerBundle.message("action.switchWorkScene.title", project.name),
            project,
            null,
        ) { choice -> SceneSwitcher.switchTo(project, choice.name) }
    }


    private fun choicesOf(project: Project?): List<SceneChoice> {
        if (project == null || project.isDisposed) return emptyList()
        val active = SceneSwitcher.activeScene(project)
        return SceneSwitcher.scenesOf(project).map { SceneChoice(it.name, it.name == active) }
    }
}

// One entry of the quick switch popup, the active scene is marked.
data class SceneChoice(val name: String, val active: Boolean) {

    override fun toString(): String =
        if (active) ProjectThemeManagerBundle.message("action.switchWorkScene.current", name) else name
}
