package com.sakurastudios.lsl

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

/**
 * Fallback: copy the current LSL file's text to the clipboard so the user
 * can paste it into the Second Life script editor manually. Useful when
 * Firestorm's external-editor watch isn't configured or available.
 */
class LslCopyToClipboardAction : AnAction("Copy LSL Script to Clipboard") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null && (vf.extension == "lsl" || vf.extension == "lslh")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        FileDocumentManager.getInstance().saveAllDocuments()
        val doc = FileDocumentManager.getInstance().getDocument(vf) ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(doc.text))

        NotificationGroupManager.getInstance().getNotificationGroup("Sakura LSL")
            .createNotification(
                "Copied to clipboard",
                "Paste into the Second Life script editor with Ctrl+V.",
                NotificationType.INFORMATION
            ).notify(project)
    }
}
