package com.sakurastudios.lsl

import com.intellij.lang.Language

/** The LSL language singleton, registered as `LSL`. */
object LslLanguage : Language("LSL") {
    private fun readResolve(): Any = LslLanguage
    override fun getDisplayName(): String = "LSL"
    override fun isCaseSensitive(): Boolean = true
}
