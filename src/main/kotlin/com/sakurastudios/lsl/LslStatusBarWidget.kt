package com.sakurastudios.lsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.IconPresentation
import com.intellij.openapi.wm.StatusBarWidget.WidgetPresentation
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Alarm
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.event.MouseEvent
import java.util.function.Consumer
import javax.swing.Icon

/**
 * Status-bar widget that shows the LSL compile status as a coloured dot.
 *
 *   green  → last compile succeeded
 *   yellow → succeeded with warnings
 *   red    → failed with errors
 *   grey   → no compile run yet for any file in this project
 *
 * Clicking the dot navigates to the first diagnostic of the most recent run.
 */
class LslStatusBarWidget(private val project: Project) : StatusBarWidget, Disposable {

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var statusBar: StatusBar? = null
    private var lastStatus: LslCompileStatus = LslCompileStatus.UNKNOWN

    override fun ID(): String = WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        schedulePoll()
    }

    override fun dispose() {
        alarm.cancelAllRequests()
        statusBar = null
    }

    override fun getPresentation(): WidgetPresentation = Presentation()

    private fun schedulePoll() {
        if (alarm.isDisposed) return
        alarm.addRequest({
            val r = LslCompileResultBus.latest(project)
            val newStatus = r?.status ?: LslCompileStatus.UNKNOWN
            if (newStatus != lastStatus) {
                lastStatus = newStatus
                statusBar?.updateWidget(WIDGET_ID)
            }
            schedulePoll()
        }, POLL_INTERVAL_MS)
    }

    private inner class Presentation : IconPresentation {
        override fun getIcon(): Icon = iconFor(lastStatus)
        override fun getTooltipText(): String = tooltipFor(lastStatus)
        override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
            ApplicationManager.getApplication().invokeLater {
                LslCompileResultBus.jumpToFirstDiagnostic(project)
            }
        }
    }

    private fun iconFor(s: LslCompileStatus): Icon = when (s) {
        LslCompileStatus.OK      -> DOT_GREEN
        LslCompileStatus.WARNING -> DOT_YELLOW
        LslCompileStatus.ERROR   -> DOT_RED
        LslCompileStatus.UNKNOWN -> DOT_GREY
    }

    private fun tooltipFor(s: LslCompileStatus): String = when (s) {
        LslCompileStatus.OK      -> "Sakura LSL: compiles cleanly"
        LslCompileStatus.WARNING -> "Sakura LSL: compiles with warnings (click for first warning)"
        LslCompileStatus.ERROR   -> "Sakura LSL: compile errors (click for first error)"
        LslCompileStatus.UNKNOWN -> "Sakura LSL: no compile run yet"
    }

    companion object {
        const val WIDGET_ID = "SakuraLsl.CompileStatus"
        private const val POLL_INTERVAL_MS = 500

        private val DOT_GREEN  = DotIcon(Color(0x40, 0xA8, 0x40))
        private val DOT_YELLOW = DotIcon(Color(0xE0, 0xB8, 0x20))
        private val DOT_RED    = DotIcon(Color(0xC8, 0x40, 0x40))
        private val DOT_GREY   = DotIcon(Color(0x80, 0x80, 0x80))
    }
}

/** A minimal 12×12 filled-circle icon. */
private class DotIcon(private val color: Color) : Icon {
    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        if (g == null) return
        val g2 = g.create() ?: return
        try {
            g2.color = color
            g2.fillOval(x + 1, y + 1, SIZE - 2, SIZE - 2)
            g2.color = color.darker()
            g2.drawOval(x + 1, y + 1, SIZE - 2, SIZE - 2)
        } finally {
            g2.dispose()
        }
    }
    override fun getIconWidth(): Int = SIZE
    override fun getIconHeight(): Int = SIZE
    companion object { private const val SIZE = 12 }
}

/** Registers the LSL status widget for every project. */
class LslStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = LslStatusBarWidget.WIDGET_ID
    override fun getDisplayName(): String = "Sakura LSL Compile Status"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = LslStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) {
        if (widget is Disposable) Disposer.dispose(widget)
    }
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
