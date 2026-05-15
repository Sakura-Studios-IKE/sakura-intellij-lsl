package com.sakurastudios.lsl

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * Right-click on a .lsl file → "Run in slemu (current file)". Creates a
 * one-off, hidden RunConfiguration that points at exactly that file, then
 * invokes it. The user never has to touch the Run Configurations dialog
 * for the common case.
 *
 * If a configuration named `LSL: <basename>` already exists, we re-use
 * it — that way users can save their per-script defaults (volume,
 * avatars, commands) and have "Run current file" pick those up next time.
 */
class LslRunCurrentFileAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val visible = file != null && (file.extension == "lsl" || file.extension == "lslh")
        e.presentation.isEnabledAndVisible = visible
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val path = file.path
        val baseName = file.nameWithoutExtension

        val runManager = RunManager.getInstance(project)
        val cfgName = "LSL: $baseName"

        // Re-use an existing configuration with this name if present.
        val existing = runManager.allSettings.firstOrNull { it.name == cfgName }
        val settings: RunnerAndConfigurationSettings = existing ?: run {
            val type = ConfigurationTypeUtil.findConfigurationType(LslRunConfigurationType::class.java)
                ?: error("LSL run-configuration type not registered")
            val factory = type.configurationFactories[0]
            val s = runManager.createConfiguration(cfgName, factory)
            (s.configuration as LslRunConfiguration).scriptPath = path
            runManager.addConfiguration(s)
            s
        }
        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
    }
}
