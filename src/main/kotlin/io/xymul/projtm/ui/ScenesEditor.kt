package io.xymul.projtm.ui

import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import io.xymul.projtm.model.ProjectSceneConfig
import io.xymul.projtm.model.Scene
import io.xymul.projtm.theme.ThemeItem
import io.xymul.projtm.theme.ThemeService
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

// Editor for the work scenes of one project, shared by both settings pages.
class ScenesEditor {

    private val model = ScenesTableModel()
    private val table = JBTable(model)
    private val panel = JPanel(BorderLayout())

    init {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.emptyText.text = ProjectThemeManagerBundle.message("settings.page.noScenes")
        table.columnModel.getColumn(1).cellEditor = ComboBoxCellEditor<ThemeItem>({ _ -> installedThemes() })
        table.addMouseListener(doubleClickEditor())
        val decorated = ToolbarDecorator.createDecorator(table)
            .setAddAction { addScene() }
            .setRemoveAction { removeScene() }
            .setAddActionName(ProjectThemeManagerBundle.message("settings.button.addScene"))
            .setRemoveActionName(ProjectThemeManagerBundle.message("settings.button.removeScene"))
            .setRemoveActionUpdater { table.selectedRow >= 0 }
            .createPanel()
        panel.add(decorated, BorderLayout.CENTER)
    }

    val component: JComponent get() = panel

    fun load(config: ProjectSceneConfig?) = model.setData(config?.scenes.orEmpty(), config?.defaultScene)

    fun scenes(): List<Scene> = model.scenes()

    fun defaultScene(): String? = model.defaultName()

    fun isModifiedAgainst(stored: ProjectSceneConfig?): Boolean {
        if (stored == null) return model.scenes().isNotEmpty()
        return stored.scenes != model.scenes() || stored.defaultScene != model.defaultName()
    }

    private fun doubleClickEditor(): MouseAdapter = object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            val row = table.rowAtPoint(event.point)
            if (row >= 0 && event.clickCount == 2) editScene(row)
        }
    }

    private fun addScene() {
        val dialog = SceneDialog(model.names())
        if (!dialog.showAndGet()) return
        val service = ThemeService.getInstance()
        val themeName = dialog.themeName() ?: service.currentThemeName() ?: return
        model.add(Scene(dialog.sceneName(), themeName))
    }

    private fun removeScene() {
        val row = table.selectedRow
        if (row >= 0) model.remove(row)
    }

    private fun editScene(row: Int) {
        val scene = model.sceneAt(row)
        val dialog = SceneDialog(model.names() - scene.name, scene)
        if (!dialog.showAndGet()) return
        val themeName = dialog.themeName() ?: scene.theme
        model.update(row, Scene(dialog.sceneName(), themeName))
    }
}
