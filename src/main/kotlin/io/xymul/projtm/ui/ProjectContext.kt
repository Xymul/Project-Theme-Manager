package io.xymul.projtm.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import java.awt.Component

// Project the given component belongs to, used inside the settings pages.
fun currentProjectOf(component: Component): Project? =
    DataManager.getInstance().getDataContext(component).getData(CommonDataKeys.PROJECT)

// Fallback for settings components that are not part of a window yet: the only open project.
fun fallbackProject(): Project? =
    ProjectManager.getInstance().openProjects.filterNot { it.isDefault }.singleOrNull()
