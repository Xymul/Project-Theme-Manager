package io.xymul.projtm.core

import com.intellij.openapi.project.Project

// Absolute project path, null for the default project.
fun projectPathOf(project: Project): String? = project.basePath
