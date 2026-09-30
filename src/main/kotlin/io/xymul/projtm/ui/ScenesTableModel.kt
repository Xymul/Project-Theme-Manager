package io.xymul.projtm.ui

import io.xymul.projtm.model.Scene
import javax.swing.table.AbstractTableModel

// Table model of the work scenes of one project.
class ScenesTableModel : AbstractTableModel() {

    private var scenes = emptyList<Scene>()
    private var defaultScene: String? = null

    fun setData(newScenes: List<Scene>, newDefault: String?) {
        scenes = newScenes
        defaultScene = newDefault
        fireTableDataChanged()
    }

    fun scenes(): List<Scene> = scenes

    fun defaultName(): String? = defaultScene

    fun sceneAt(row: Int): Scene = scenes[row]

    fun add(scene: Scene) {
        scenes = scenes + scene
        if (defaultScene == null) defaultScene = scene.name
        fireTableDataChanged()
    }

    fun remove(row: Int) {
        val removed = scenes[row]
        scenes = scenes.filterIndexed { index, _ -> index != row }
        if (defaultScene == removed.name) defaultScene = scenes.firstOrNull()?.name
        fireTableDataChanged()
    }

    fun update(row: Int, scene: Scene) {
        if (defaultScene == scenes[row].name) defaultScene = scene.name
        scenes = scenes.mapIndexed { index, current -> if (index == row) scene else current }
        fireTableRowsUpdated(row, row)
    }

    fun names(): Set<String> = scenes.map { it.name }.toSet()

    override fun getRowCount(): Int = scenes.size

    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String = when (column) {
        0 -> ProjectThemeManagerBundle.message("settings.column.sceneName")
        1 -> ProjectThemeManagerBundle.message("settings.column.theme")
        else -> ProjectThemeManagerBundle.message("settings.column.isDefault")
    }

    override fun getColumnClass(columnIndex: Int): Class<*> =
        if (columnIndex == 2) Boolean::class.javaObjectType else Any::class.java

    override fun isCellEditable(row: Int, column: Int): Boolean = column != 0

    override fun getValueAt(row: Int, column: Int): Any? {
        val scene = scenes[row]
        return when (column) {
            0 -> scene.name
            1 -> themeItem(scene.themeId)
            else -> scene.name == defaultScene
        }
    }

    override fun setValueAt(value: Any?, row: Int, column: Int) {
        when (column) {
            1 -> {
                val theme = value as? io.xymul.projtm.theme.ThemeItem ?: return
                scenes = scenes.mapIndexed { index, scene ->
                    if (index == row) scene.copy(themeId = theme.id, dark = theme.dark) else scene
                }
                fireTableRowsUpdated(row, row)
            }
            2 -> {
                defaultScene = if (value == true) scenes[row].name else null
                fireTableDataChanged()
            }
        }
    }
}
