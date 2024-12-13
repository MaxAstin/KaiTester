package com.tester.kai.action

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.tester.kai.service.TestGenerationService

private const val FAMILY_NAME = "Testing"
private const val ACTION_NAME = "Generate test by AI"

class GenerateTestAction : CodeInsightAction(), CodeInsightActionHandler, IntentionAction {

    override fun getHandler(): CodeInsightActionHandler {
        return this
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return isAvailable(
            project = project,
            editor = editor,
            file = file
        )
    }

    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getFamilyName(): String {
        return FAMILY_NAME
    }

    override fun getText(): String {
        return ACTION_NAME
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val generateTestService = TestGenerationService.getInstance(project)
        return generateTestService.isTestGenerationAvailable(
            file = file,
            editor = editor,
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return

        val generateTestService = TestGenerationService.getInstance(project)
        generateTestService.generateTest(
            editor = editor,
            file = file
        )
    }

}