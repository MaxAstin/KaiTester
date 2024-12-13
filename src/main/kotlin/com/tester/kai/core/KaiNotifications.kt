package com.tester.kai.core

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

/**
 * Common notifications interface
 */
object KaiNotifications {

    private const val GROUP_ID = "com.tester.kai.notifications"
    private const val TITLE = "KaiTester"

    @Volatile
    private var project: Project? = null

    fun setup(project: Project) {
        this.project = project
    }

    fun info(message: String) {
        sendNotification(message, NotificationType.INFORMATION)
    }

    fun warning(message: String) {
        sendNotification(message, NotificationType.WARNING)
    }

    fun error(exception: Throwable) {
        val message = exception.message ?: "Something went wrong"
        error(message = message)
    }

    fun error(message: String) {
        sendNotification(message, NotificationType.ERROR)
    }

    private fun sendNotification(message: String, type: NotificationType, action: AnAction? = null) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(TITLE, message, type)
        action?.let { notification.addAction(action) }

        notification.notify(project)
    }

}