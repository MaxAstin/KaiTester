package com.tester.kai.core.util

import com.intellij.openapi.application.ApplicationManager

inline fun <T>  runWriteAction(
    crossinline action: () -> T,
): T? {
    var result: T? = null
    ApplicationManager.getApplication().runWriteAction {
        result = action()
    }
    return result
}