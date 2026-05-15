package com.sakurastudios.lsl

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import javax.swing.Icon

/**
 * Run configuration type: "LSL: lsltest run".
 *
 * Targets a directory containing `test_*.py` files and runs the Sakura
 * `lsltest` test runner over them, streaming results to the run console.
 */
class LsltestRunConfigurationType : ConfigurationType {

    private val factory = LsltestRunConfigurationFactory(this)

    override fun getDisplayName(): String = "LSL: lsltest run"
    override fun getConfigurationTypeDescription(): String =
        "Run a directory of test_*.py files through the Sakura lsltest harness."
    override fun getIcon(): Icon = LslIcons.FILE
    override fun getId(): String = "SAKURA_LSL_LSLTEST_RUN_CONFIG"
    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)
}

class LsltestRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "SAKURA_LSL_LSLTEST_FACTORY"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        LsltestRunConfiguration(project, this, "lsltest")

    override fun getName(): String = "LSL: lsltest run"
}
