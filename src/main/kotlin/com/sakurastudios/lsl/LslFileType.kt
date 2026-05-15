package com.sakurastudios.lsl

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/** File type for `.lsl` and `.lslh` files. */
class LslFileType private constructor() : LanguageFileType(LslLanguage) {

    override fun getName(): String = "LSL"
    override fun getDescription(): String = "Linden Scripting Language"
    override fun getDefaultExtension(): String = "lsl"
    override fun getIcon(): Icon = LslIcons.FILE

    companion object {
        @JvmField
        val INSTANCE: LslFileType = LslFileType()
    }
}
