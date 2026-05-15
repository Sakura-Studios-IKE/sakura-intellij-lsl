package com.sakurastudios.lsl

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * Sidebar tool window with two tabs:
 *
 *   1. "Emulator" — fast button to run the currently open LSL script in
 *      slemu, live JSON event log, "send command" input that pipes
 *      player-action commands to slemu's --commands stream.
 *   2. "Debugger" — embeds an lsldb REPL: spawns the binary, exposes
 *      stdin/stdout in a JTextArea, surfaces stopped-/caught- events.
 *
 * Designed to be lightweight: no XDebugger integration yet (that would
 * require a PSI parser for full editor integration). What's here is
 * enough for a developer to inspect, step, and intervene without
 * leaving the IDE.
 */
class LslToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val cf = ContentFactory.getInstance()
        toolWindow.contentManager.addContent(cf.createContent(EmulatorPanel(project), "Emulator", false))
        toolWindow.contentManager.addContent(cf.createContent(DebuggerPanel(project), "Debugger", false))
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}

/* ------------------------------ Emulator pane ----------------------------- */

private class EmulatorPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val scriptField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("LSL Script", "", project,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }
    private val output = JTextArea().apply { isEditable = false; lineWrap = false }
    private val cmdInput = JBTextField()
    private var process: Process? = null
    private var stdinWriter: PrintWriter? = null

    init {
        // Top: file + run button + send-command bar
        val top = JPanel(BorderLayout()).apply {
            val controls = JPanel(GridLayout(0, 1, 4, 4))
            val row1 = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Script:"), scriptField, 1, false).panel
            controls.add(row1)
            val row2 = JPanel(BorderLayout(4, 4))
            row2.add(JBLabel("Send:"), BorderLayout.WEST)
            row2.add(cmdInput, BorderLayout.CENTER)
            controls.add(row2)
            val buttons = JPanel()
            buttons.add(JButton("Run").apply { addActionListener { run() } })
            buttons.add(JButton("Stop").apply { addActionListener { stop() } })
            buttons.add(JButton("Clear").apply { addActionListener { output.text = "" } })
            controls.add(buttons)
            add(controls, BorderLayout.CENTER)
        }
        add(top, BorderLayout.NORTH)
        add(JBScrollPane(output), BorderLayout.CENTER)
        cmdInput.addActionListener { sendCommand() }
    }

    private fun sendCommand() {
        val t = cmdInput.text.trim()
        if (t.isEmpty()) return
        stdinWriter?.let { w ->
            w.println(t)
            w.flush()
            append("> $t\n")
        }
        cmdInput.text = ""
    }

    private fun append(text: String) {
        SwingUtilities.invokeLater {
            output.append(text)
            output.caretPosition = output.text.length
        }
    }

    private fun stop() {
        process?.let { p -> try { p.destroy() } catch (_: Exception) {} }
        stdinWriter = null
        process = null
        append("\n[stopped]\n")
    }

    private fun run() {
        stop()
        val script = scriptField.text.trim()
        if (script.isBlank() || !File(script).exists()) {
            append("[error] no script selected\n"); return
        }
        val settings = LslcSettings.getInstance()
        val lslc = settings.lslcPath.ifBlank { "lslc" }
        val slemu = settings.slemuPath.ifBlank { "slemu" }

        // Compile
        append("$ $lslc -c $script\n")
        val compile = try {
            ProcessBuilder(listOf(lslc, "-c", "-fno-color", script)).redirectErrorStream(true).start()
        } catch (e: Exception) {
            append("[error] cannot invoke $lslc: ${e.message}\n"); return
        }
        val out = compile.inputStream.bufferedReader().readText()
        val rc = compile.waitFor()
        if (out.isNotBlank()) append(out + "\n")
        if (rc != 0) { append("[lslc failed rc=$rc]\n"); return }

        val bc = stripLslExtension(script) + ".lslbc"
        append("$ $slemu --json-events $bc\n")
        val pb = ProcessBuilder(listOf(slemu, "--json-events", bc))
            .redirectErrorStream(true)
        pb.directory(File(script).parentFile)
        val p = try { pb.start() } catch (e: Exception) {
            append("[error] cannot invoke $slemu: ${e.message}\n"); return
        }
        process = p
        stdinWriter = PrintWriter(p.outputStream)
        Thread({
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            reader.forEachLine { append(it + "\n") }
            append("[process exited rc=${p.waitFor()}]\n")
        }, "Sakura-Slemu-Reader").apply { isDaemon = true; start() }
    }
}

/* ------------------------------ Debugger pane ----------------------------- */

private class DebuggerPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val scriptField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("LSL Script", "", project,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }
    private val output = JTextArea().apply { isEditable = false }
    private val cmdInput = JBTextField()
    private var process: Process? = null
    private var stdinWriter: PrintWriter? = null

    init {
        val top = JPanel(GridLayout(0, 1, 4, 4))
        top.add(FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Script:"), scriptField, 1, false).panel)
        val cmdRow = JPanel(BorderLayout(4, 4))
        cmdRow.add(JBLabel("lsldb> "), BorderLayout.WEST)
        cmdRow.add(cmdInput, BorderLayout.CENTER)
        top.add(cmdRow)
        val buttons = JPanel()
        for ((label, fn) in listOf(
            "Start" to ::start,
            "Continue" to { send("continue") },
            "Step" to { send("step") },
            "Locals" to { send("locals") },
            "Globals" to { send("globals") },
            "Snapshot" to { send("snapshot") },
            "Stop" to ::stop,
            "Clear" to { output.text = "" }
        )) {
            buttons.add(JButton(label).apply { addActionListener { fn() } })
        }
        top.add(buttons)
        add(top, BorderLayout.NORTH)
        add(JBScrollPane(output), BorderLayout.CENTER)
        cmdInput.addActionListener {
            val t = cmdInput.text.trim()
            if (t.isNotEmpty()) { send(t); cmdInput.text = "" }
        }
    }

    private fun append(s: String) {
        SwingUtilities.invokeLater {
            output.append(s)
            output.caretPosition = output.text.length
        }
    }

    private fun send(line: String) {
        val w = stdinWriter ?: run { append("[no live session]\n"); return }
        append("> $line\n")
        w.println(line); w.flush()
    }

    private fun stop() {
        process?.let { try { it.destroy() } catch (_: Exception) {} }
        process = null; stdinWriter = null
        append("\n[lsldb stopped]\n")
    }

    private fun start() {
        stop()
        val script = scriptField.text.trim()
        if (script.isBlank() || !File(script).exists()) { append("[error] no script selected\n"); return }
        val settings = LslcSettings.getInstance()
        val lslc = settings.lslcPath.ifBlank { "lslc" }
        val slemu = settings.slemuPath.ifBlank { "slemu" }
        // lsldb path: same dir as slemu, or "lsldb" on PATH.
        val slemuFile = File(slemu)
        val lsldbPath = (slemuFile.parentFile?.resolve("lsldb"))?.takeIf { it.canExecute() }?.absolutePath
            ?: (slemuFile.parentFile?.parentFile?.resolve("sakura-lsldb/lsldb"))?.takeIf { it.canExecute() }?.absolutePath
            ?: "lsldb"

        // Compile first.
        append("$ $lslc -c $script\n")
        val compile = try { ProcessBuilder(listOf(lslc, "-c", "-fno-color", script)).redirectErrorStream(true).start() }
            catch (e: Exception) { append("[error] $lslc: ${e.message}\n"); return }
        val cOut = compile.inputStream.bufferedReader().readText()
        if (cOut.isNotBlank()) append(cOut + "\n")
        if (compile.waitFor() != 0) { append("[compile failed]\n"); return }

        val bc = stripLslExtension(script) + ".lslbc"
        append("$ $lsldbPath --slemu $slemu --source $script -- $bc\n")
        val pb = ProcessBuilder(listOf(lsldbPath, "--slemu", slemu, "--source", script, "--", bc))
            .redirectErrorStream(true)
        pb.directory(File(script).parentFile)
        val p = try { pb.start() } catch (e: Exception) { append("[error] $lsldbPath: ${e.message}\n"); return }
        process = p
        stdinWriter = PrintWriter(p.outputStream)
        Thread({
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            reader.forEachLine { append(it + "\n") }
            append("[lsldb exited rc=${p.waitFor()}]\n")
        }, "Sakura-Lsldb-Reader").apply { isDaemon = true; start() }
    }
}
