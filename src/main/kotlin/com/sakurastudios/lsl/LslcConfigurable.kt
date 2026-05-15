package com.sakurastudios.lsl

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings page under "Tools → Sakura LSL".
 *
 * Lets the user point the plugin at their `lslc`, `slemu`, and `lsltest`
 * binaries, configure the Firestorm external-editor watch directory, and
 * provide extra flags for `lslc`.
 */
class LslcConfigurable : Configurable {

    private var rootPanel: JPanel? = null

    private val lslcField        = textWithBrowse("Choose lslc binary")
    private val slemuField       = textWithBrowse("Choose slemu binary")
    private val lsldbField       = textWithBrowse("Choose lsldb binary")
    private val lsltestField     = textWithBrowse("Choose lsltest binary")
    private val watchDirField    = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "Firestorm Watch Directory",
            "The directory Firestorm watches for external editor changes.",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }
    private val extraFlagsField  = JBTextField()

    override fun getDisplayName(): String = "Sakura LSL"

    override fun createComponent(): JComponent {
        val info = JBLabel(
            "<html><body style='width:480px'>" +
            "<b>Sakura LSL</b> wires IntelliJ up to the Sakura LSL toolchain.<br/>" +
            "Set the paths to the compiler, emulator, and test runner below.<br/><br/>" +
            "The <b>Firestorm watch directory</b> is the folder Firestorm's external-editor " +
            "feature is configured to watch. When you trigger <i>Hot Reload to Second Life</i>, " +
            "Sakura LSL writes the current script there and Firestorm pushes it into the " +
            "in-world script. Note: Second Life has no public upload API, so you must " +
            "already be logged into Firestorm with the script's edit window open." +
            "</body></html>"
        )

        val builder = FormBuilder.createFormBuilder()
            .addComponent(info)
            .addSeparator()
            .addLabeledComponent(JBLabel("lslc path:"), lslcField, 1, false)
            .addLabeledComponent(JBLabel("slemu path:"), slemuField, 1, false)
            .addLabeledComponent(JBLabel("lsldb path:"), lsldbField, 1, false)
            .addLabeledComponent(JBLabel("lsltest path:"), lsltestField, 1, false)
            .addLabeledComponent(JBLabel("Firestorm watch directory:"), watchDirField, 1, false)
            .addLabeledComponent(JBLabel("Extra lslc flags:"), extraFlagsField, 1, false)
            .addComponentFillVertically(JPanel(), 0)

        val panel = builder.panel
        panel.border = JBUI.Borders.empty(10)
        rootPanel = panel
        reset()
        return panel
    }

    private fun textWithBrowse(title: String): TextFieldWithBrowseButton =
        TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                title, "", null,
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            )
        }

    override fun isModified(): Boolean {
        val s = LslcSettings.getInstance()
        return s.lslcPath          != lslcField.text ||
               s.slemuPath         != slemuField.text ||
               s.lsldbPath         != lsldbField.text ||
               s.lslTestPath       != lsltestField.text ||
               s.firestormWatchDir != watchDirField.text ||
               s.extraLslcFlags    != extraFlagsField.text
    }

    override fun apply() {
        val s = LslcSettings.getInstance()
        s.lslcPath          = lslcField.text
        s.slemuPath         = slemuField.text
        s.lsldbPath         = lsldbField.text
        s.lslTestPath       = lsltestField.text
        s.firestormWatchDir = watchDirField.text
        s.extraLslcFlags    = extraFlagsField.text
    }

    override fun reset() {
        val s = LslcSettings.getInstance()
        lslcField.text        = s.lslcPath
        slemuField.text       = s.slemuPath
        lsldbField.text       = s.lsldbPath
        lsltestField.text     = s.lslTestPath
        watchDirField.text    = s.firestormWatchDir
        extraFlagsField.text  = s.extraLslcFlags
    }

    override fun disposeUIResources() {
        rootPanel = null
    }
}
