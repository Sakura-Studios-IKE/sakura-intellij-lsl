package com.sakurastudios.lsl

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/** File type for `.lsl` and `.lslh` files. */
class LslFileType private constructor() : LanguageFileType(LslLanguage) {

    // The file type's machine name MUST be globally unique. If we used a
    // plain "LSL" here we'd collide with other community LSL plugins that
    // already claim that name. The user-visible display still says "LSL".
    override fun getName(): String = "Sakura LSL"
    override fun getDisplayName(): String = "LSL"
    override fun getDescription(): String = "Linden Scripting Language (Sakura toolchain)"
    override fun getDefaultExtension(): String = "lsl"
    override fun getIcon(): Icon = LslIcons.FILE

    companion object {
        @JvmField
        val INSTANCE: LslFileType = LslFileType()
    }
}
