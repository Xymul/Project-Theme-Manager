package io.xymul.projtm.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import io.xymul.projtm.core.ConfigRepository
import io.xymul.projtm.core.projectPathOf
import io.xymul.projtm.model.ProjectEntry
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.theme.SceneSwitcher
import io.xymul.projtm.theme.ThemeService
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

// Settings page that edits the work scenes of any other registered project.
class OtherScenesConfigurable : Configurable {

    private val editor = ScenesEditor()
    private val projectBox = ComboBox<ProjectEntry>()
    private val infoLabel = JBLabel()
    private val disposable = Disposer.newDisposable("ProjectThemeManager")
    private val onRepositoryChanged: () -> Unit = { reloadProjects() }

    private var selectedEntry: ProjectEntry? = null
    private var storedConfig: ProjectSceneConfig? = null

    override fun getDisplayName(): String = ProjectThemeManagerBundle.message("settings.page.otherScenes")

    override fun createComponent(): JComponent {
        projectBox.renderer = ProjectEntryRenderer()
        projectBox.addActionListener { onProjectSelected() }
        val header = JPanel(BorderLayout())
        header.add(JBLabel(ProjectThemeManagerBundle.message("settings.page.project")), BorderLayout.WEST)
        header.add(projectBox, BorderLayout.CENTER)
        header.add(infoLabel, BorderLayout.EAST)
        val panel = JPanel(BorderLayout())
        panel.add(header, BorderLayout.NORTH)
        panel.add(editor.component, BorderLayout.CENTER)
        ConfigRepository.getInstance().addChangeListener(onRepositoryChanged)
        ThemeService.getInstance().subscribe(disposable) { loadSelection() }
        reloadProjects()
        return panel
    }

    override fun isModified(): Boolean = editor.isModifiedAgainst(storedConfig)

    override fun apply() {
        val entry = selectedEntry ?: return
        val repository = ConfigRepository.getInstance()
        val stored = storedConfig
        val config = ProjectSceneConfig(entry.name, entry.path, entry.uuid, editor.defaultScene(), editor.scenes())
        repository.updateScenes(entry, config)
        storedConfig = config
        // Only the project of the current window may switch the theme right away.
        val project = currentProjectOf(editor.component) ?: return
        if (projectPathOf(project) == entry.path && SceneSwitcher.shouldApply(project, stored, config)) {
            config.defaultScene?.let { SceneSwitcher.switchTo(project, it) }
        }
    }

    override fun reset() = reloadProjects()

    override fun disposeUIResources() {
        ConfigRepository.getInstance().removeChangeListener(onRepositoryChanged)
        Disposer.dispose(disposable)
    }

    private fun reloadProjects() {
        val projects = ConfigRepository.getInstance().projects()
        val previous = selectedEntry?.uuid
        projectBox.removeAllItems()
        projects.forEach { projectBox.addItem(it) }
        val restored = projects.firstOrNull { it.uuid == previous } ?: projects.firstOrNull()
        selectedEntry = restored
        projectBox.selectedItem = restored
        infoLabel.text = if (projects.isEmpty()) ProjectThemeManagerBundle.message("settings.page.noProjects") else ""
        loadSelection()
    }

    private fun onProjectSelected() {
        selectedEntry = projectBox.selectedItem as? ProjectEntry
        loadSelection()
    }

    private fun loadSelection() {
        val config = selectedEntry?.let { ConfigRepository.getInstance().scenesOf(it) }
        storedConfig = config
        editor.load(config)
    }
}

// Shows the project name followed by its path.
private class ProjectEntryRenderer : ListCellRenderer<ProjectEntry> {

    override fun getListCellRendererComponent(
        list: JList<out ProjectEntry>,
        value: ProjectEntry?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val label = JLabel(if (value == null) "" else value.name + "  " + value.path)
        label.isOpaque = true
        label.background = if (isSelected) list.selectionBackground else list.background
        label.foreground = if (isSelected) list.selectionForeground else list.foreground
        return label
    }
}
