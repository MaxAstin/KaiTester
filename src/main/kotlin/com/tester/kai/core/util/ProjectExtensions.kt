package com.tester.kai.core.util

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory

fun Project.createKtFile(name: String, content: String): KtFile {
    return KtPsiFactory(project = this)
        .createFile(
            fileName = name,
            text = content
        )
}

fun Project.createEmptyKtFile(
    name: String,
    packageName: String,
): KtFile {
    return createKtFile(
        name = name,
        content = "package $packageName"
    )
}

fun Project.createEmptyKtClass(name: String): KtClass {
    return createKtClass("class $name {}")
}

fun Project.createKtClass(content: String): KtClass {
    return KtPsiFactory(project = this)
        .createClass(text = content)
}

fun Project.createKtFunction(content: String): KtFunction {
    return KtPsiFactory(project = this)
        .createFunction(funDecl = content)
}