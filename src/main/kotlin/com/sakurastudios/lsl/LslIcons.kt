package com.sakurastudios.lsl

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Centralised icon loader for the Sakura LSL plugin.
 *
 * The two icons differ only in intrinsic SVG dimensions:
 *  - FILE        is 16x16 — the IntelliJ standard for file-type, action,
 *                 completion and run-configuration icons.
 *  - TOOL_WINDOW is 13x13 — the IntelliJ standard for tool-window stripe
 *                 icons. Using FILE here causes the stripe (and any
 *                 component that respects intrinsic SVG size) to render
 *                 the mark at its source dimensions and distort the
 *                 surrounding chrome.
 */
object LslIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/lsl.svg", LslIcons::class.java)

    @JvmField
    val TOOL_WINDOW: Icon = IconLoader.getIcon("/icons/lsl_toolwindow.svg", LslIcons::class.java)
}
