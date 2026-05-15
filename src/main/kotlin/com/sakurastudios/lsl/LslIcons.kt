package com.sakurastudios.lsl

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Centralised icon loader for the Sakura LSL plugin. */
object LslIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/lsl.svg", LslIcons::class.java)
}
