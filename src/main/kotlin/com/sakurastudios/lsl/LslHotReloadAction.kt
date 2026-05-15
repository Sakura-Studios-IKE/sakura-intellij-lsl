package com.sakurastudios.lsl

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.File

/**
 * Hot reload to Second Life via the Firestorm external-editor watch pattern.
 *
 * Steps:
 *   1. The user has Firestorm running, with the script's edit dialog open
 *      and the "external editor" feature pointed at a path Sakura LSL
 *      writes to.
 *   2. The user invokes this action; the plugin writes the current LSL file
 *      to <firestormWatchDir>/<filename>.
 *   3. Firestorm detects the change and pushes it into the in-world script.
 *
 * If no watch directory has been configured the action shows a balloon with
 * instructions linking to the settings page.
 */
class LslHotReloadAction : AnAction("Hot Reload to Second Life") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            vf != null && (vf.extension == "lsl" || vf.extension == "lslh")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val settings = LslcSettings.getInstance()

        val notifier = NotificationGroupManager.getInstance().getNotificationGroup("Sakura LSL")

        if (settings.firestormWatchDir.isBlank()) {
            notifier.createNotification(
                "Hot reload unavailable",
                "Set <b>Firestorm watch directory</b> in <i>Settings → Tools → Sakura LSL</i>. " +
                    "In Firestorm, enable <i>Preferences → Network & Files → External editor</i> " +
                    "and point it at the same directory.",
                NotificationType.WARNING
            ).notify(project)
            return
        }

        val dir = File(settings.firestormWatchDir)
        if (!dir.isDirectory && !dir.mkdirs()) {
            notifier.createNotification(
                "Hot reload failed",
                "Watch directory does not exist and could not be created: ${dir.absolutePath}",
                NotificationType.ERROR
            ).notify(project)
            return
        }

        // Make sure on-disk content is the buffer's current state.
        FileDocumentManager.getInstance().saveAllDocuments()

        val source = File(vf.path)
        val target = File(dir, vf.name)
        try {
            source.copyTo(target, overwrite = true)
        } catch (ex: Exception) {
            notifier.createNotification(
                "Hot reload failed",
                "Could not write to ${target.absolutePath}: ${ex.message}",
                NotificationType.ERROR
            ).notify(project)
            return
        }

        notifier.createNotification(
            "Hot reload sent",
            "Wrote <code>${target.absolutePath}</code>. Make sure the matching script's " +
                "edit dialog is open in Firestorm for the change to take effect.",
            NotificationType.INFORMATION
        ).notify(project)
    }
}
