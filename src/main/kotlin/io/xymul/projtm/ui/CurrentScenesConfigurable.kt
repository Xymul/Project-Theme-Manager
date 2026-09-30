package io.xymul.projtm.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.projectPathOf
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.theme.SceneSwitcher
import io.xymul.projtm.theme.ThemeService
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

// Settings page that edits the work scenes of the current project.
class CurrentScenesConfigurable : Configurable {

    private val editor = ScenesEditor()
    private val projectLabel = JBLabel()
    private val hintLabel = JBLabel()
    private val disposable = Disposer.newDisposable("ProjectThemeManager")
    private val onRepositoryChanged: () -> Unit = { reset() }

    // Configuration as it was loaded, used to detect changes that require a theme switch.
    private var storedConfig: ProjectSceneConfig? = null

    override fun getDisplayName(): String =
        currentProject()?.name ?: ProjectThemeManagerBundle.message("settings.page.currentScenes")

    override fun createComponent(): JComponent {
        val header = JPanel(BorderLayout())
        header.add(projectLabel, BorderLayout.WEST)
        header.add(hintLabel, BorderLayout.EAST)
        val panel = JPanel(BorderLayout())
        panel.add(header, BorderLayout.NORTH)
        panel.add(editor.component, BorderLayout.CENTER)
        ConfigRepository.getInstance().addChangeListener(onRepositoryChanged)
        ThemeService.getInstance().subscribe(disposable) { reset() }
        reset()
        return panel
    }

    override fun isModified(): Boolean {
        val project = currentProject() ?: return editor.isModifiedAgainst(null)
        return editor.isModifiedAgainst(entryFor(project)?.let { ConfigRepository.getInstance().scenesOf(it) })
    }

    override fun apply() {
        val project = currentProject() ?: return
        val path = projectPathOf(project) ?: return
        val repository = ConfigRepository.getInstance()
        val entry = entryFor(project) ?: run {
            val first = editor.scenes().firstOrNull() ?: return
            repository.addProject(project.name, path, first)
        }
        val stored = storedConfig
        val config = ProjectSceneConfig(entry.name, path, entry.uuid, editor.defaultScene(), editor.scenes())
        repository.updateScenes(entry, config)
        storedConfig = config
        if (SceneSwitcher.shouldApply(project, stored, config)) {
            config.defaultScene?.let { SceneSwitcher.switchTo(project, it) }
        }
    }

    override fun reset() {
        val repository = ConfigRepository.getInstance()
        val project = currentProject()
        if (project == null) {
            projectLabel.text = ProjectThemeManagerBundle.message("settings.page.noProject")
            hintLabel.text = ""
            storedConfig = null
            editor.load(null)
            return
        }
        val path = projectPathOf(project).orEmpty()
        val entry = repository.findProject(path)
        projectLabel.text = project.name + " (" + path + ")"
        hintLabel.text = if (entry == null) {
            ProjectThemeManagerBundle.message("settings.page.notRegistered")
        } else {
            ProjectThemeManagerBundle.message("settings.page.hint")
        }
        val config = entry?.let { repository.scenesOf(it) }
        storedConfig = config
        editor.load(config)
    }

    override fun disposeUIResources() {
        ConfigRepository.getInstance().removeChangeListener(onRepositoryChanged)
        Disposer.dispose(disposable)
    }

    private fun currentProject(): Project? = currentProjectOf(editor.component) ?: fallbackProject()

    private fun entryFor(project: Project): ProjectEntry? {
        val path = projectPathOf(project) ?: return null
        return ConfigRepository.getInstance().findProject(path)
    }
}
