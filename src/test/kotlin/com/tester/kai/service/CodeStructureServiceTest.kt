package com.tester.kai.service

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tester.kai.core.model.CodeStructure

class CodeStructureServiceTest : BasePlatformTestCase() {

    private val rawCode: String
        get() {
            return """
                import com.first.Class1
                import com.second.Class2
                
                @Annotation
                fun foo() { return a + b }
        
                class MyClass {
        
                    private val field1: Int = 1
                    private val field2: String = ""
        
                    suspend fun `boo oo`() = runBlocking { foo() }
                }
            """.trimIndent()
        }

    fun testGetCodeStructure() {
        val codeStructureService = project.service<CodeStructureService>()
        val expected = CodeStructure(
            imports = listOf(
                "com.first.Class1",
                "com.second.Class2"
            ),
            functions = listOf(
                CodeStructure.Function(
                    annotations = listOf("@Annotation"),
                    name = "foo",
                    body = "{ return a + b }"
                )
            ),
            classes = listOf(
                CodeStructure.Class(
                    functions = listOf(
                        CodeStructure.Function(
                            annotations = emptyList(),
                            name = "boo oo",
                            body = "{ foo() }"
                        )
                    )
                )
            ),
        )

        val result = codeStructureService.getCodeStructure(rawCode = rawCode)

        assertEquals(expected, result)
    }

}