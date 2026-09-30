package io.xymul.projtm.ui

import com.intellij.ide.DataManager
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.awt.Component

// Project of the settings component, otherwise the project the IDE currently works on.
fun currentProjectOf(component: Component?): Project? =
    component?.let { DataManager.getInstance().getDataContext(it).getData(CommonDataKeys.PROJECT) }
        ?: activeProject()

// The active project of the IDE, or the only project that is open.
fun activeProject(): Project? {
    ProjectUtil.getActiveProject()?.let { return it }
    val open = ProjectUtil.getOpenProjects().filterNot { it.isDefault }
    return open.singleOrNull() ?: open.firstOrNull()
}
