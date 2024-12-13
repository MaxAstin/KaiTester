package com.tester.kai.core.util

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

val PsiFile.filePackage: String?
    get() {
        return (this as? KtFile)?.packageFqName?.asString()
    }

fun PsiFile.findParentDirectoryByName(directoryName: String): PsiDirectory? {
    var currentDirectory = parent
    while (currentDirectory != null) {
        if (currentDirectory.name == directoryName) {
            return currentDirectory
        }
        currentDirectory = currentDirectory.parent
    }

    return null
}

fun PsiDirectory.findOrCreateSubdirectoryByName(directoryName: String): PsiDirectory {
    return findSubdirectory(directoryName)
        ?: runWriteAction { createSubdirectory(directoryName) }
        ?: error("Can't create subdirectory $directoryName in $name")
}