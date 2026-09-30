package io.xymul.projtm.ui

import io.xymul.projtm.theme.ThemeItem
import io.xymul.projtm.theme.ThemeService
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

// A theme is shown and stored by its name only.
fun ThemeItem.displayName(): String = name

fun installedThemes(): List<ThemeItem> = ThemeService.getInstance().themes()

fun themeItem(themeName: String): ThemeItem = ThemeItem(themeName)

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
