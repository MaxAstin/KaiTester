package com.tester.kai

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.tester.kai.core.KaiNotifications

class KaiStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        DumbService.getInstance(project).runWhenSmart {
            setupNotifications(project)
        }
    }

    private fun setupNotifications(project: Project) {
        KaiNotifications.setup(project)
    }

}