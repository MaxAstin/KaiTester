package com.tester.kai.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.tester.kai.core.model.CodeStructure
import com.tester.kai.core.util.createKtFile
import org.jetbrains.kotlin.psi.*

@Service(Service.Level.PROJECT)
class CodeStructureService(
    private val project: Project,
) {

    companion object {
        private const val TEMP_FILE_NAME = "Temp.kt"

        fun getInstance(project: Project): CodeStructureService = project.service()
    }

    fun getCodeStructure(rawCode: String): CodeStructure {
        val ktFile = project.createKtFile(name = TEMP_FILE_NAME, content = rawCode)
        return getCodeStructure(ktFile = ktFile)
    }

    private fun getCodeStructure(ktFile: KtFile): CodeStructure {
        val imports = ktFile.importDirectives.map {
            it.importPath.toString()
        }
        val functions = ktFile.declarations.functions
        val classes = ktFile.declarations
            .filterIsInstance<KtClass>()
            .map { ktClass ->
                CodeStructure.Class(
                    functions = ktClass.declarations.functions
                )
            }

        return CodeStructure(
            imports = imports,
            functions = functions,
            classes = classes
        )
    }

    private val List<KtDeclaration>.functions: List<CodeStructure.Function>
        get() {
            return filterIsInstance<KtNamedFunction>()
                .map { function ->
                    val bodyExpression = function.bodyExpression
                    CodeStructure.Function(
                        annotations = function.modifierList?.annotationEntries?.map { annotation ->
                            annotation.text
                        }.orEmpty(),
                        name = function.name.orEmpty(),
                        body = if (bodyExpression is KtCallExpression) {
                            bodyExpression.lambdaArguments.firstOrNull()
                                ?.getLambdaExpression()
                                ?.text
                                .orEmpty()
                        } else {
                            function.bodyBlockExpression?.text.orEmpty()
                        }
                    )
                }
        }

}