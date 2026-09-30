package io.xymul.projtm.ui

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.awt.Component

// Project the given component belongs to, used inside the settings pages.
fun currentProjectOf(component: Component): Project? =
    DataManager.getInstance().getDataContext(component).getData(CommonDataKeys.PROJECT)

// Absolute project path, null for the default project.
fun projectPathOf(project: Project): String? = project.basePath
