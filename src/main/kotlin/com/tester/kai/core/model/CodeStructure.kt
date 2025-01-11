package com.tester.kai.core.model

data class CodeStructure(
    val imports: List<String>,
    val functions: List<Function>,
    val classes: List<Class>,
) {

    data class Function(
        val annotations: List<String>,
        val name: String,
        val body: String,
    )

    data class Class(
        val functions: List<Function>,
    )

    fun getAllFunctions(): List<Function> {
        return functions + classes.flatMap { aClass ->
            aClass.functions
        }
    }

}