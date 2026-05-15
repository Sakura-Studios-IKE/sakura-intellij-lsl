package com.sakurastudios.lsl

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * "LSL: run in slemu" run configuration.
 *
 * - Stores the path to the `.lsl` source and an optional list of CLI args for
 *   the emulator.
 * - On run, invokes `<lslc> <flags> <script>.lsl` and then `<slemu> <script>`,
 *   piping output to the standard run-tool-window console.
 */
class LslRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<LslRunConfiguration>(project, factory, name) {

    /** Path to the .lsl file to run. */
    var scriptPath: String = ""

    /** Extra flags handed to slemu. */
    var slemuArgs: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = LslRunConfigurationEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        SlemuRunState(this, environment)

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "scriptPath", scriptPath)
        JDOMExternalizerUtil.writeField(element, "slemuArgs", slemuArgs)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        scriptPath = JDOMExternalizerUtil.readField(element, "scriptPath") ?: ""
        slemuArgs  = JDOMExternalizerUtil.readField(element, "slemuArgs") ?: ""
    }
}

/** The execution state for a slemu run. */
class SlemuRunState(
    private val cfg: LslRunConfiguration,
    environment: ExecutionEnvironment
) : CommandLineState(environment) {

    init {
        consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project)
    }

    override fun startProcess(): ProcessHandler {
        val settings = LslcSettings.getInstance()
        val script = cfg.scriptPath
        if (script.isBlank()) {
            throw ExecutionException("No LSL script path configured for this run configuration.")
        }
        val scriptFile = java.io.File(script)
        if (!scriptFile.exists()) {
            throw ExecutionException("LSL script does not exist: $script")
        }

        // 1) Compile with lslc. We invoke it synchronously and surface failure
        //    as an ExecutionException so the user sees it in the run console.
        val lslc = settings.lslcPath.ifBlank { "lslc" }
        val compileCmd = mutableListOf(lslc, "--fno-color")
        if (settings.extraLslcFlags.isNotBlank()) {
            compileCmd += settings.extraLslcFlags.split(Regex("\\s+")).filter { it.isNotBlank() }
        }
        compileCmd += script
        val compile = try {
            ProcessBuilder(compileCmd).redirectErrorStream(true).start()
        } catch (e: Exception) {
            throw ExecutionException("Could not invoke '$lslc': ${e.message}", e)
        }
        val compileOut = compile.inputStream.bufferedReader().readText()
        val compileRc = compile.waitFor()
        if (compileRc != 0) {
            throw ExecutionException(
                "lslc exited with status $compileRc.\n$compileOut"
            )
        }

        // 2) Hand off to slemu.
        val slemu = settings.slemuPath.ifBlank { "slemu" }
        val cmd = GeneralCommandLine(slemu)
        cmd.addParameter(script)
        if (cfg.slemuArgs.isNotBlank()) {
            for (a in cfg.slemuArgs.split(Regex("\\s+")).filter { it.isNotBlank() }) {
                cmd.addParameter(a)
            }
        }
        cmd.charset = Charsets.UTF_8
        cmd.setWorkDirectory(scriptFile.parentFile?.absolutePath ?: ".")

        val handler = OSProcessHandler(cmd)
        ProcessTerminatedListener.attach(handler)
        return handler
    }
}

/** The Swing editor presented in the Run Configurations dialog. */
class LslRunConfigurationEditor : SettingsEditor<LslRunConfiguration>() {

    private val scriptField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "LSL Script", "", null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        )
    }
    private val argsField = JBTextField()
    private val panel: JPanel by lazy { build() }

    private fun build(): JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("LSL script:"), scriptField, 1, false)
        .addLabeledComponent(JBLabel("slemu args:"), argsField, 1, false)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    override fun resetEditorFrom(s: LslRunConfiguration) {
        scriptField.text = s.scriptPath
        argsField.text   = s.slemuArgs
    }

    override fun applyEditorTo(s: LslRunConfiguration) {
        s.scriptPath = scriptField.text
        s.slemuArgs  = argsField.text
    }

    override fun createEditor(): JComponent = panel
}
