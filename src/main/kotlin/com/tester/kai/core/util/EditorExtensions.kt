package com.tester.kai.core.util

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction

val Editor.currentKtClass: KtClass?
    get() {
        return findParentPsiElementByClass(KtClass::class.java)
    }

val Editor.currentKtFunction: KtFunction?
    get() {
        return findParentPsiElementByClass(KtFunction::class.java)
    }

private fun <T : PsiElement> Editor.findParentPsiElementByClass(aClass: Class<T>): T? {
    val element = PsiUtilBase.getElementAtCaret(this) ?: return null
    return PsiTreeUtil.getParentOfType(element, aClass)
}