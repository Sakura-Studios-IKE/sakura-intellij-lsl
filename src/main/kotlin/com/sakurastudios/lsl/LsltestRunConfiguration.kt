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
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/** "LSL: lsltest run" run configuration. */
class LsltestRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<LsltestRunConfiguration>(project, factory, name) {

    /** Directory containing `test_*.py` files. */
    var testDir: String = ""

    /** Extra args appended to the lsltest command line. */
    var extraArgs: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        LsltestRunConfigurationEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        LsltestRunState(this, environment)

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "testDir", testDir)
        JDOMExternalizerUtil.writeField(element, "extraArgs", extraArgs)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        testDir   = JDOMExternalizerUtil.readField(element, "testDir") ?: ""
        extraArgs = JDOMExternalizerUtil.readField(element, "extraArgs") ?: ""
    }
}

class LsltestRunState(
    private val cfg: LsltestRunConfiguration,
    environment: ExecutionEnvironment
) : CommandLineState(environment) {

    init {
        consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project)
    }

    override fun startProcess(): ProcessHandler {
        val settings = LslcSettings.getInstance()
        val dir = cfg.testDir
        if (dir.isBlank()) {
            throw ExecutionException("No test directory configured for this run configuration.")
        }
        val testDir = File(dir)
        if (!testDir.isDirectory) {
            throw ExecutionException("Test directory does not exist: $dir")
        }

        val lsltest = settings.lslTestPath.ifBlank { "lsltest" }
        val cmd = GeneralCommandLine(lsltest, "run", dir)
        if (cfg.extraArgs.isNotBlank()) {
            for (a in cfg.extraArgs.split(Regex("\\s+")).filter { it.isNotBlank() }) {
                cmd.addParameter(a)
            }
        }
        cmd.charset = Charsets.UTF_8
        cmd.setWorkDirectory(dir)

        val handler = try {
            OSProcessHandler(cmd)
        } catch (e: Exception) {
            throw ExecutionException("Could not invoke '$lsltest': ${e.message}", e)
        }
        ProcessTerminatedListener.attach(handler)
        return handler
    }
}

class LsltestRunConfigurationEditor : SettingsEditor<LsltestRunConfiguration>() {

    private val dirField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "Test Directory", "", null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }
    private val argsField = JBTextField()
    private val panel: JPanel by lazy { build() }

    private fun build(): JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Test directory:"), dirField, 1, false)
        .addLabeledComponent(JBLabel("Extra args:"), argsField, 1, false)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    override fun resetEditorFrom(s: LsltestRunConfiguration) {
        dirField.text  = s.testDir
        argsField.text = s.extraArgs
    }

    override fun applyEditorTo(s: LsltestRunConfiguration) {
        s.testDir   = dirField.text
        s.extraArgs = argsField.text
    }

    override fun createEditor(): JComponent = panel
}
