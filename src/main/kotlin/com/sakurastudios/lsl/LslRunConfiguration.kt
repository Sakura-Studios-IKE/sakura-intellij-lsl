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
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import org.jdom.Element
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * "LSL: run in slemu" run configuration.
 *
 * Stores every knob slemu accepts so a developer can drive a full
 * scenario from one tab of the Run/Debug Configurations dialog without
 * memorising slemu's CLI flags.
 */
class LslRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<LslRunConfiguration>(project, factory, name) {

    /** Path to the .lsl file to run. */
    var scriptPath: String = ""

    /** Volume directory (slemu --volume). Blank = slemu default. */
    var volumePath: String = ""

    /** If true, slemu shells out to curl (real HTTP). */
    var useRealHttp: Boolean = false

    /** Optional fixture file path. Used unless useRealHttp is true. */
    var httpFixturePath: String = ""

    /** Emit one JSON object per emitted event. Recommended ON for tool integration. */
    var jsonEvents: Boolean = true

    /** Log every event dispatch. */
    var trace: Boolean = false

    /** Owner avatar — UUID and starting L$. Blank uuid = slemu default (1111...). */
    var ownerUuid: String = ""
    var ownerName: String = ""
    var ownerBalance: Int = 100

    /** Step / wall caps. */
    var maxSteps: Int = 100000
    var wallTimeout: Int = 60

    /** Object name slemu reports for llGetObjectName. */
    var objectName: String = ""

    /** Extra avatars as one-per-line entries  `UUID:BALANCE:Name`. */
    var avatarsText: String = ""

    /** Path to a pre-written commands file, OR (if blank) the inline body below. */
    var commandsFilePath: String = ""
    var commandsInline: String = ""

    /** Raw catch-all extra flags. */
    var extraSlemuArgs: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = LslRunConfigurationEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        SlemuRunState(this, environment)

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        with(element) {
            JDOMExternalizerUtil.writeField(this, "scriptPath", scriptPath)
            JDOMExternalizerUtil.writeField(this, "volumePath", volumePath)
            JDOMExternalizerUtil.writeField(this, "useRealHttp", useRealHttp.toString())
            JDOMExternalizerUtil.writeField(this, "httpFixturePath", httpFixturePath)
            JDOMExternalizerUtil.writeField(this, "jsonEvents", jsonEvents.toString())
            JDOMExternalizerUtil.writeField(this, "trace", trace.toString())
            JDOMExternalizerUtil.writeField(this, "ownerUuid", ownerUuid)
            JDOMExternalizerUtil.writeField(this, "ownerName", ownerName)
            JDOMExternalizerUtil.writeField(this, "ownerBalance", ownerBalance.toString())
            JDOMExternalizerUtil.writeField(this, "maxSteps", maxSteps.toString())
            JDOMExternalizerUtil.writeField(this, "wallTimeout", wallTimeout.toString())
            JDOMExternalizerUtil.writeField(this, "objectName", objectName)
            JDOMExternalizerUtil.writeField(this, "avatarsText", avatarsText)
            JDOMExternalizerUtil.writeField(this, "commandsFilePath", commandsFilePath)
            JDOMExternalizerUtil.writeField(this, "commandsInline", commandsInline)
            JDOMExternalizerUtil.writeField(this, "extraSlemuArgs", extraSlemuArgs)
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        scriptPath      = JDOMExternalizerUtil.readField(element, "scriptPath") ?: ""
        volumePath      = JDOMExternalizerUtil.readField(element, "volumePath") ?: ""
        useRealHttp     = (JDOMExternalizerUtil.readField(element, "useRealHttp") ?: "false") == "true"
        httpFixturePath = JDOMExternalizerUtil.readField(element, "httpFixturePath") ?: ""
        jsonEvents      = (JDOMExternalizerUtil.readField(element, "jsonEvents") ?: "true") == "true"
        trace           = (JDOMExternalizerUtil.readField(element, "trace") ?: "false") == "true"
        ownerUuid       = JDOMExternalizerUtil.readField(element, "ownerUuid") ?: ""
        ownerName       = JDOMExternalizerUtil.readField(element, "ownerName") ?: ""
        ownerBalance    = (JDOMExternalizerUtil.readField(element, "ownerBalance") ?: "100").toIntOrNull() ?: 100
        maxSteps        = (JDOMExternalizerUtil.readField(element, "maxSteps") ?: "100000").toIntOrNull() ?: 100000
        wallTimeout     = (JDOMExternalizerUtil.readField(element, "wallTimeout") ?: "60").toIntOrNull() ?: 60
        objectName      = JDOMExternalizerUtil.readField(element, "objectName") ?: ""
        avatarsText     = JDOMExternalizerUtil.readField(element, "avatarsText") ?: ""
        commandsFilePath = JDOMExternalizerUtil.readField(element, "commandsFilePath") ?: ""
        commandsInline  = JDOMExternalizerUtil.readField(element, "commandsInline") ?: ""
        extraSlemuArgs  = JDOMExternalizerUtil.readField(element, "extraSlemuArgs") ?: ""
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
        if (script.isBlank()) throw ExecutionException("No LSL script path configured for this run configuration.")
        val scriptFile = File(script)
        if (!scriptFile.exists()) throw ExecutionException("LSL script does not exist: $script")

        // 1) Compile with lslc.
        val lslc = settings.lslcPath.ifBlank { "lslc" }
        val compileCmd = mutableListOf(lslc, "-c", "-fno-color")
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
        if (compileRc != 0) throw ExecutionException("lslc exited with status $compileRc.\n$compileOut")

        // 2) Path of the produced .lslbc.
        val bytecodePath = stripLslExtension(script) + ".lslbc"

        // 3) If the user provided inline commands AND no commands file path,
        //    materialise the inline text into a sibling .cmds temp.
        val effectiveCommandsFile = when {
            cfg.commandsFilePath.isNotBlank() -> cfg.commandsFilePath
            cfg.commandsInline.isNotBlank() -> {
                val f = File.createTempFile("sakura-cmds-", ".cmds")
                f.writeText(cfg.commandsInline)
                f.deleteOnExit()
                f.absolutePath
            }
            else -> ""
        }

        // 4) Assemble the slemu invocation.
        val slemu = settings.slemuPath.ifBlank { "slemu" }
        val cmd = GeneralCommandLine(slemu)
        if (cfg.jsonEvents) cmd.addParameter("--json-events")
        if (cfg.trace) cmd.addParameter("--trace")
        if (cfg.useRealHttp) cmd.addParameter("--http-real")
        else if (cfg.httpFixturePath.isNotBlank()) {
            cmd.addParameter("--http-fixture"); cmd.addParameter(cfg.httpFixturePath)
        }
        if (cfg.volumePath.isNotBlank()) {
            cmd.addParameter("--volume"); cmd.addParameter(cfg.volumePath)
        }
        if (cfg.ownerUuid.isNotBlank()) {
            val ownerArg = if (cfg.ownerName.isBlank()) cfg.ownerUuid else "${cfg.ownerUuid}:${cfg.ownerName}"
            cmd.addParameter("--owner"); cmd.addParameter(ownerArg)
        }
        cmd.addParameter("--owner-balance"); cmd.addParameter(cfg.ownerBalance.toString())
        if (cfg.objectName.isNotBlank()) {
            cmd.addParameter("--name"); cmd.addParameter(cfg.objectName)
        }
        cmd.addParameter("--steps"); cmd.addParameter(cfg.maxSteps.toString())
        cmd.addParameter("--timeout"); cmd.addParameter(cfg.wallTimeout.toString())
        // Avatars: one --avatar per non-blank line.
        cfg.avatarsText.lines().forEach { line ->
            val t = line.trim()
            if (t.isNotEmpty() && !t.startsWith("#")) {
                cmd.addParameter("--avatar"); cmd.addParameter(t)
            }
        }
        if (effectiveCommandsFile.isNotBlank()) {
            cmd.addParameter("--commands"); cmd.addParameter(effectiveCommandsFile)
        }
        if (cfg.extraSlemuArgs.isNotBlank()) {
            for (a in cfg.extraSlemuArgs.split(Regex("\\s+")).filter { it.isNotBlank() }) cmd.addParameter(a)
        }
        cmd.addParameter(bytecodePath)
        cmd.charset = Charsets.UTF_8
        cmd.setWorkDirectory(scriptFile.parentFile?.absolutePath ?: ".")

        val handler = OSProcessHandler(cmd)
        ProcessTerminatedListener.attach(handler)
        return handler
    }
}

/** Strip a trailing .lsl/.lslh/.txt extension. */
fun stripLslExtension(s: String): String {
    val lower = s.lowercase()
    for (ext in listOf(".lsl", ".lslh", ".txt")) {
        if (lower.endsWith(ext)) return s.substring(0, s.length - ext.length)
    }
    return s
}

/** Rich Swing editor — replaces the bare two-field UI. */
class LslRunConfigurationEditor : SettingsEditor<LslRunConfiguration>() {

    private val scriptField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("LSL Script", "", null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }
    private val volumeField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Project Volume Directory", "", null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor())
    }
    private val fixtureField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("HTTP Fixture File", "", null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }
    private val commandsField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Commands (.cmds) File", "", null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
    }
    private val httpRealBox = JBCheckBox("Use real HTTP (curl)")
    private val jsonEventsBox = JBCheckBox("Emit JSON-line events", true)
    private val traceBox = JBCheckBox("--trace every event")
    private val ownerUuidField = JBTextField()
    private val ownerNameField = JBTextField()
    private val ownerBalanceField = JBTextField("100")
    private val maxStepsField = JBTextField("100000")
    private val timeoutField = JBTextField("60")
    private val objectNameField = JBTextField()
    private val avatarsArea = JBTextArea(4, 60).apply {
        toolTipText = "One avatar per line, format: UUID:BALANCE:Name  (lines starting with # are comments)"
    }
    private val commandsArea = JBTextArea(7, 60).apply {
        toolTipText = "Pre-defined player actions. One per line. " +
                "If a commands file is also set, the file takes precedence."
    }
    private val extraArgsField = JBTextField()
    private val panel: JPanel by lazy { build() }

    private fun build(): JPanel {
        val avatarsScroll = JBScrollPane(avatarsArea)
        val cmdsScroll = JBScrollPane(commandsArea)
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("LSL script:"), scriptField, 1, false)
            .addLabeledComponent(JBLabel("Object name (llGetObjectName):"), objectNameField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Volume directory:"), volumeField, 1, false)
            .addComponent(jsonEventsBox)
            .addComponent(traceBox)
            .addSeparator()
            .addComponent(JBLabel("HTTP backend:"))
            .addComponent(httpRealBox)
            .addLabeledComponent(JBLabel("Fixture file (used when not real):"), fixtureField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Owner UUID:"), ownerUuidField, 1, false)
            .addLabeledComponent(JBLabel("Owner name:"), ownerNameField, 1, false)
            .addLabeledComponent(JBLabel("Owner balance (L\$):"), ownerBalanceField, 1, false)
            .addLabeledComponent(JBLabel("Extra avatars (UUID:BAL:Name per line):"), avatarsScroll, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Player commands file:"), commandsField, 1, false)
            .addLabeledComponent(JBLabel("Or inline commands:"), cmdsScroll, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Max events:"), maxStepsField, 1, false)
            .addLabeledComponent(JBLabel("Timeout (s):"), timeoutField, 1, false)
            .addLabeledComponent(JBLabel("Extra slemu flags:"), extraArgsField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun resetEditorFrom(s: LslRunConfiguration) {
        scriptField.text  = s.scriptPath
        volumeField.text  = s.volumePath
        httpRealBox.isSelected = s.useRealHttp
        fixtureField.text = s.httpFixturePath
        jsonEventsBox.isSelected = s.jsonEvents
        traceBox.isSelected = s.trace
        ownerUuidField.text = s.ownerUuid
        ownerNameField.text = s.ownerName
        ownerBalanceField.text = s.ownerBalance.toString()
        maxStepsField.text = s.maxSteps.toString()
        timeoutField.text  = s.wallTimeout.toString()
        objectNameField.text = s.objectName
        avatarsArea.text  = s.avatarsText
        commandsField.text = s.commandsFilePath
        commandsArea.text = s.commandsInline
        extraArgsField.text = s.extraSlemuArgs
    }

    override fun applyEditorTo(s: LslRunConfiguration) {
        s.scriptPath = scriptField.text
        s.volumePath = volumeField.text
        s.useRealHttp = httpRealBox.isSelected
        s.httpFixturePath = fixtureField.text
        s.jsonEvents = jsonEventsBox.isSelected
        s.trace = traceBox.isSelected
        s.ownerUuid = ownerUuidField.text
        s.ownerName = ownerNameField.text
        s.ownerBalance = ownerBalanceField.text.toIntOrNull() ?: 100
        s.maxSteps = maxStepsField.text.toIntOrNull() ?: 100000
        s.wallTimeout = timeoutField.text.toIntOrNull() ?: 60
        s.objectName = objectNameField.text
        s.avatarsText = avatarsArea.text
        s.commandsFilePath = commandsField.text
        s.commandsInline = commandsArea.text
        s.extraSlemuArgs = extraArgsField.text
    }

    override fun createEditor(): JComponent = panel
}
