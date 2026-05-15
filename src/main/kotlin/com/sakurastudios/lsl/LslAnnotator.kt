package com.sakurastudios.lsl

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Runs `sakura-lslc` on the saved buffer and surfaces gcc-style diagnostics
 * inline as IntelliJ annotations.
 *
 * The annotator is invoked by the platform after the user pauses typing.
 * `collectInformation()` snapshots the file; `doAnnotate()` runs lslc on a
 * temp copy; `apply()` posts annotations back onto the document.
 *
 * Also publishes a [LslCompileResult] to [LslCompileResultBus] so the
 * status-bar widget can refresh its compile light.
 */
class LslAnnotator : ExternalAnnotator<LslAnnotator.CompileInfo, LslAnnotator.CompileInfo>() {

    /** Snapshot of a buffer + project for an annotation run. */
    data class CompileInfo(
        val project: Project,
        val virtualFile: VirtualFile,
        val text: String,
        val diagnostics: MutableList<LslDiagnostic> = mutableListOf(),
        var compilerInvocationError: String? = null
    )

    /** A single parsed compiler diagnostic. */
    data class LslDiagnostic(
        val line: Int,           // 1-based
        val column: Int,         // 1-based, 0 if absent
        val severity: HighlightSeverity,
        val message: String
    )

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): CompileInfo? {
        val vfile = file.virtualFile ?: return null
        if (vfile.extension !in LSL_EXTENSIONS) return null
        return CompileInfo(file.project, vfile, editor.document.text)
    }

    override fun collectInformation(file: PsiFile): CompileInfo? {
        val vfile = file.virtualFile ?: return null
        if (vfile.extension !in LSL_EXTENSIONS) return null
        val doc = FileDocumentManager.getInstance().getDocument(vfile) ?: return null
        return CompileInfo(file.project, vfile, doc.text)
    }

    override fun doAnnotate(info: CompileInfo): CompileInfo {
        val settings = LslcSettings.getInstance()
        val lslc = settings.lslcPath.ifBlank { "lslc" }

        // Write the buffer to a temp file with the same extension lslc expects.
        val ext = info.virtualFile.extension ?: "lsl"
        val tmp: File = try {
            val f = Files.createTempFile("sakura-lsl-", ".$ext").toFile()
            f.writeText(info.text, StandardCharsets.UTF_8)
            f
        } catch (e: Exception) {
            info.compilerInvocationError = "Failed to write temp file: ${e.message}"
            return info
        }

        try {
            val command = mutableListOf(lslc, "--fno-color", "-Wall")
            if (settings.extraLslcFlags.isNotBlank()) {
                command += settings.extraLslcFlags.split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
            }
            command += tmp.absolutePath

            val pb = ProcessBuilder(command).redirectErrorStream(true)
            val proc = try {
                pb.start()
            } catch (e: Exception) {
                info.compilerInvocationError =
                    "Could not invoke '$lslc' — check the Sakura LSL settings page. (${e.message})"
                return info
            }

            val output = proc.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            val finished = proc.waitFor(LSLC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                info.compilerInvocationError = "lslc timed out after $LSLC_TIMEOUT_SECONDS s"
                return info
            }

            parseDiagnostics(output, tmp.absolutePath, info.diagnostics)
            return info
        } finally {
            try { tmp.delete() } catch (_: Exception) {}
        }
    }

    override fun apply(file: PsiFile, info: CompileInfo, holder: AnnotationHolder) {
        val doc = FileDocumentManager.getInstance().getDocument(info.virtualFile) ?: return

        info.compilerInvocationError?.let { msg ->
            LOG.info(msg)
            // Surface as a file-level annotation so it doesn't need a non-empty range.
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, msg)
                .fileLevel()
                .create()
        }

        for (d in info.diagnostics) {
            val range = lineColToRange(doc, d.line, d.column) ?: continue
            holder.newAnnotation(d.severity, d.message)
                .range(range)
                .create()
        }

        // Publish overall compile status for the status-bar widget.
        val status = when {
            info.diagnostics.any { it.severity == HighlightSeverity.ERROR }   -> LslCompileStatus.ERROR
            info.diagnostics.any { it.severity == HighlightSeverity.WARNING } -> LslCompileStatus.WARNING
            else                                                              -> LslCompileStatus.OK
        }
        LslCompileResultBus.update(
            info.project,
            LslCompileResult(info.virtualFile, status, info.diagnostics.toList())
        )
    }

    // ----------- diagnostic parsing -----------

    /**
     * Parse gcc-style diagnostics. Accepts both:
     *   path:line:col: severity: message
     *   path:line: severity: message
     *
     * The path component is whatever filename lslc embeds; we don't try to
     * remap it. Lines that don't match this shape are ignored.
     */
    private fun parseDiagnostics(
        output: String,
        tmpPath: String,
        sink: MutableList<LslDiagnostic>
    ) {
        for (line in output.lineSequence()) {
            val m = GCC_DIAG.find(line) ?: continue
            // Anchor to our temp file only — ignore diagnostics from #included files.
            val emittedPath = m.groupValues[1]
            if (emittedPath != tmpPath && File(emittedPath).absolutePath != tmpPath) continue

            val lineNo = m.groupValues[2].toIntOrNull() ?: continue
            val colNo  = m.groupValues[3].toIntOrNull() ?: 0
            val sev    = when (m.groupValues[4].lowercase()) {
                "error", "fatal", "fatal error" -> HighlightSeverity.ERROR
                "warning"                       -> HighlightSeverity.WARNING
                "note"                          -> HighlightSeverity.WEAK_WARNING
                "info"                          -> HighlightSeverity.INFORMATION
                else                            -> HighlightSeverity.WARNING
            }
            val msg = m.groupValues[5].trim()
            sink += LslDiagnostic(lineNo, colNo, sev, msg)
        }
    }

    /** Convert a 1-based line/col pair to a [TextRange] in the editor document. */
    private fun lineColToRange(doc: Document, line: Int, col: Int): TextRange? {
        val zeroLine = (line - 1).coerceIn(0, doc.lineCount - 1)
        val lineStart = doc.getLineStartOffset(zeroLine)
        val lineEnd   = doc.getLineEndOffset(zeroLine)
        val effectiveStart = (lineStart + maxOf(0, col - 1)).coerceAtMost(lineEnd)
        // Highlight at least one character, prefer up to end-of-line.
        val end = if (effectiveStart < lineEnd) lineEnd else (effectiveStart + 1).coerceAtMost(doc.textLength)
        return if (effectiveStart < end) TextRange(effectiveStart, end) else null
    }

    companion object {
        private val LOG = Logger.getInstance(LslAnnotator::class.java)
        private val LSL_EXTENSIONS = setOf("lsl", "lslh")
        private const val LSLC_TIMEOUT_SECONDS = 10L

        // path:line:col: severity: message     (col optional)
        private val GCC_DIAG = Regex(
            """^(.+?):(\d+)(?::(\d+))?: (error|fatal error|warning|note|info): (.*)$""",
            RegexOption.IGNORE_CASE
        )
    }
}

// --------- compile-result bus shared with the status-bar widget ---------

enum class LslCompileStatus { UNKNOWN, OK, WARNING, ERROR }

data class LslCompileResult(
    val file: VirtualFile,
    val status: LslCompileStatus,
    val diagnostics: List<LslAnnotator.LslDiagnostic>
)

/**
 * Project-keyed bag holding the most recent compile result.
 * Listeners poll it on a short timer (cheap, no message-bus plumbing needed).
 */
object LslCompileResultBus {
    private val byProject: MutableMap<String, LslCompileResult> = java.util.concurrent.ConcurrentHashMap()

    fun update(project: Project, result: LslCompileResult) {
        byProject[project.locationHash] = result
    }

    fun latest(project: Project): LslCompileResult? = byProject[project.locationHash]

    fun clear(project: Project) {
        byProject.remove(project.locationHash)
    }

    /** Jump to the first diagnostic of the latest run, if any. */
    fun jumpToFirstDiagnostic(project: Project) {
        val r = latest(project) ?: return
        val first = r.diagnostics.firstOrNull() ?: return
        val editorManager = FileEditorManager.getInstance(project)
        val descriptor = com.intellij.openapi.fileEditor.OpenFileDescriptor(
            project, r.file, (first.line - 1).coerceAtLeast(0), (first.column - 1).coerceAtLeast(0)
        )
        descriptor.navigate(true)
        editorManager.openFile(r.file, true)
    }
}
