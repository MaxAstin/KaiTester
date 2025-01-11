package com.tester.kai.service

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.tester.kai.assistant.data.AssistantRepositoryProvider
import com.tester.kai.core.KaiNotifications
import com.tester.kai.core.model.CodeStructure
import com.tester.kai.core.util.*
import com.tester.kai.model.RawTest
import com.tester.kai.model.TestableTarget
import kotlinx.coroutines.*
import org.jetbrains.kotlin.idea.base.psi.imports.addImport
import org.jetbrains.kotlin.idea.search.usagesSearch.constructor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.isPublic

@Service(Service.Level.PROJECT)
class TestGenerationService(
    private val project: Project,
    private val serviceScope: CoroutineScope,
) {

    companion object {

        fun getInstance(project: Project): TestGenerationService = project.service()

        private const val TEST_GENERATING_TITLE = "Generating test by AI"
        private const val SRC_DIRECTORY = "src"
        private const val TEST_DIRECTORY = "test"
        private const val KOTLIN_DIRECTORY = "kotlin"
        private const val TEST_ANNOTATION = "@Test"
        private const val RUN_TEST_IMPORT = "kotlinx.coroutines.test.runTest"
    }

    private val assistantRepository = AssistantRepositoryProvider.assistantRepository

    fun isTestGenerationAvailable(file: PsiFile, editor: Editor): Boolean {
        if (file !is KtFile) {
            return false
        }

        return editor.currentKtFunction?.isPublic ?: false
    }

    fun generateTest(
        editor: Editor,
        file: PsiFile,
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            exception.printStackTrace()
            KaiNotifications.error(exception)
        }
        serviceScope.launch(exceptionHandler) {
            withModalProgress(
                project = project,
                title = TEST_GENERATING_TITLE
            ) {
                reportSequentialProgress(100) { reporter ->
                    val testableTarget = reporter.nextStep(10) {
                        getTestableTarget(
                            editor = editor,
                            file = file
                        )
                    }

                    val rawTest = reporter.nextStep(75) {
                        assistantRepository.getRawTest(
                            testableTarget = testableTarget
                        ).getOrThrow()
                    }

                    val testStructure = reporter.nextStep(80) {
                        getTestStructure(rawTest = rawTest)
                    }

                    val testDirectory = reporter.nextStep(85) {
                        findOrCreateTestDirectory(file = file)
                    }

                    val testFile = reporter.nextStep(90) {
                        getTestFile(
                            testDirectory = testDirectory,
                            testableTarget = testableTarget
                        ) ?: createTestFile(
                            testableTarget = testableTarget,
                            testDirectory = testDirectory,
                        ) ?: createTestFileError()
                    }

                    val testClass = reporter.nextStep(95) {
                        getTestClass(
                            testFile = testFile,
                            testableTarget = testableTarget
                        )
                    }

                    reporter.nextStep(99) {
                        updateTestFile(
                            testClass = testClass,
                            testableTarget = testableTarget,
                            testFile = testFile,
                            testStructure = testStructure,
                        )
                    }

                    openTestFile(
                        testDirectory = testDirectory,
                        testFile = testFile
                    )
                }
            }
        }
    }

    private fun getTestableTarget(
        editor: Editor,
        file: PsiFile,
    ): TestableTarget {
        return runReadAction {
            editor.currentKtFunction?.takeIf { function ->
                function.isPublic
            }?.let { function ->
                val imports = (file as? KtFile)?.importDirectives?.mapNotNull { import ->
                    import.importedFqName?.toString()
                }.orEmpty()
                val isSuspend = function.modifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true
                val ktClass = editor.currentKtClass
                val constructor = ktClass?.let {
                    buildString {
                        append(ktClass.name)
                        append("(")
                        ktClass.constructor?.valueParameters.orEmpty().forEachIndexed { index, parameter ->
                            if (index > 0) {
                                append(", ")
                            }
                            append("${parameter.name}: ${parameter.type}")
                        }
                        append(")")
                    }
                }
                TestableTarget(
                    imports = imports,
                    isSuspend = isSuspend,
                    functionCode = function.text,
                    classConstructor = constructor,
                    className = ktClass?.name.orEmpty(),
                    packageName = file.filePackage.orEmpty(),
                )
            } ?: noFunctionFoundError()
        }
    }

    private fun getTestStructure(rawTest: RawTest): CodeStructure {
        val codeStructureService = CodeStructureService.getInstance(project = project)
        val rawTestCode = rawTest.code.fixUpRawCode()
        return runReadAction {
            codeStructureService.getCodeStructure(rawCode = rawTestCode)
        }
    }

    private fun buildTestFunctionContent(
        function: CodeStructure.Function,
        forSuspend: Boolean,
    ): String {
        return buildString {
            appendLine("@Test")
            append("fun `${function.name}`()")
            if (forSuspend) {
                append("= runTest")
            }
            appendLine(function.body)
            appendLine()
        }
    }

    private fun String.fixUpRawCode(): String {
        return lines()
            .drop(1)
            .dropLast(1)
            .joinToString("\n")
    }

    private suspend fun findOrCreateTestDirectory(file: PsiFile): PsiDirectory {
        return withContext(Dispatchers.EDT) {
            var directory: PsiDirectory? = null
            WriteCommandAction.runWriteCommandAction(project) {
                val srcDirectory =
                    file.findParentDirectoryByName(directoryName = SRC_DIRECTORY) ?: createTestFileError()
                directory = srcDirectory
                    .findOrCreateSubdirectoryByName(directoryName = TEST_DIRECTORY)
                    .findOrCreateSubdirectoryByName(directoryName = KOTLIN_DIRECTORY)
                val packageParts = file.filePackage?.split('.') ?: createTestFileError()
                packageParts.forEach { part ->
                    directory = directory?.findOrCreateSubdirectoryByName(directoryName = part)
                }
            }
            directory ?: findCreateTestDirectoryError()
        }
    }

    private suspend fun getTestFile(
        testDirectory: PsiDirectory,
        testableTarget: TestableTarget,
    ): KtFile? {
        val testFileName = "${testableTarget.className}Test.kt"
        return readAction {
            testDirectory.findFile(testFileName) as? KtFile
        }
    }

    private fun getTestClass(
        testFile: KtFile,
        testableTarget: TestableTarget,
    ): KtClass? {
        val className = "${testableTarget.className}Test"
        return runReadAction {
            testFile.declarations
                .filterIsInstance<KtClass>()
                .firstOrNull { ktClass ->
                    ktClass.name == className
                }
        }
    }

    private suspend fun createTestFile(
        testableTarget: TestableTarget,
        testDirectory: PsiDirectory,
    ): KtFile? {
        val testFileName = "${testableTarget.className}Test.kt"
        return withContext(Dispatchers.EDT) {
            WriteCommandAction.runWriteCommandAction(project) {
                project.createEmptyKtFile(
                    name = testFileName,
                    packageName = testableTarget.packageName
                ).also { file ->
                    testDirectory.add(file)
                }
            }
            testDirectory.refresh()
            testDirectory.findFile(testFileName) as? KtFile
        }
    }

    private suspend fun updateTestFile(
        testClass: KtClass?,
        testableTarget: TestableTarget,
        testFile: KtFile,
        testStructure: CodeStructure,
    ) {
        withContext(Dispatchers.EDT) {
            WriteCommandAction.runWriteCommandAction(project) {
                // Imports
                testableTarget.imports.onEach { import ->
                    testFile.addImport(FqName(import))
                }
                testStructure.imports.onEach { import ->
                    testFile.addImport(FqName(import))
                }
                if (testableTarget.isSuspend) {
                    testFile.addImport(FqName(RUN_TEST_IMPORT))
                }

                // Class
                val targetClass = if (testClass == null) {
                    val testClassName = "${testableTarget.className}Test"
                    val createdClass = project.createEmptyKtClass(name = testClassName)
                    testFile.add(createdClass) as KtClass
                } else {
                    testClass
                }

                // Functions
                testStructure.getAllFunctions().filter { function ->
                    function.annotations.contains(TEST_ANNOTATION)
                }.onEach { function ->
                    val content = buildTestFunctionContent(
                        function = function,
                        forSuspend = testableTarget.isSuspend
                    )
                    val ktFunction = project.createKtFunction(content = content)
                    targetClass.addDeclaration(declaration = ktFunction)
                }
                OptimizeImportsProcessor(project, testFile).run()
                testFile.reformatFile()
            }
        }
    }

    private suspend fun PsiDirectory.refresh() {
        withContext(Dispatchers.EDT) {
            virtualFile.refresh(false, false)
        }
    }

    private suspend fun openTestFile(
        testDirectory: PsiDirectory,
        testFile: KtFile,
    ) {
        withContext(Dispatchers.EDT) {
            testDirectory.virtualFile.refresh(false, false)
            val virtualFile = testDirectory.virtualFile.findChild(testFile.name)
            if (virtualFile != null) {
                FileEditorManager.getInstance(project)
                    .openFile(virtualFile, true)
            }
        }
    }

    private fun PsiFile.reformatFile() {
        CodeStyleManager.getInstance(project).reformat(this)
    }

    private fun noFunctionFoundError(): Nothing {
        error("No function found to test")
    }

    private fun findCreateTestDirectoryError(): Nothing {
        error("Failed to find/create test directory")
    }

    private fun createTestFileError(): Nothing {
        error("Failed to create test file")
    }

}