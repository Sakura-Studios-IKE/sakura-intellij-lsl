package com.sakurastudios.lsl

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import javax.swing.Icon

/**
 * Run configuration type: "LSL: run in slemu".
 *
 * Compiles an LSL file with `lslc` and pipes the resulting bytecode (or
 * source, depending on slemu's accepted input) into `slemu`, streaming
 * `slemu`'s JSON event log to the run console.
 */
class LslRunConfigurationType : ConfigurationType {

    private val factory = LslRunConfigurationFactory(this)

    override fun getDisplayName(): String = "LSL: run in slemu"
    override fun getConfigurationTypeDescription(): String =
        "Compile an LSL script with lslc and execute it in the Sakura SL emulator (slemu)."
    override fun getIcon(): Icon = LslIcons.FILE
    override fun getId(): String = "SAKURA_LSL_SLEMU_RUN_CONFIG"
    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)
}

class LslRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "SAKURA_LSL_SLEMU_FACTORY"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        LslRunConfiguration(project, this, "LSL in slemu")

    override fun getName(): String = "LSL: run in slemu"
}
