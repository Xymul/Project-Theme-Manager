package io.xymul.projtm.ui

import io.xymul.projtm.theme.ThemeItem
import io.xymul.projtm.theme.ThemeService
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

fun ThemeItem.displayName(): String = if (dark) "$name (dark)" else name

fun installedThemes(): List<ThemeItem> = ThemeService.getInstance().themes()

fun themeItem(themeId: String): ThemeItem {
    val installed = installedThemes().firstOrNull { it.id == themeId }
    if (installed != null) return installed
    return ThemeItem(themeId, ProjectThemeManagerBundle.message("settings.theme.missing", themeId), false)
}

class ThemeItemRenderer : ListCellRenderer<ThemeItem> {

    override fun getListCellRendererComponent(
        list: JList<out ThemeItem>,
        value: ThemeItem?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val label = JLabel(value?.displayName().orEmpty())
        label.isOpaque = true
        label.background = if (isSelected) list.selectionBackground else list.background
        label.foreground = if (isSelected) list.selectionForeground else list.foreground
        return label
    }
}
