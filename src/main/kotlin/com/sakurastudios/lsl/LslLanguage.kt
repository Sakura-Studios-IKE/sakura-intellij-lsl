package com.sakurastudios.lsl

import com.intellij.lang.Language

/**
 * The LSL language singleton.
 *
 * Registered with a **vendor-qualified ID** (`sakura-lsl`, *not* the bare
 * `LSL`) so this plugin can coexist with other community LSL plugins —
 * notably `io.github.riej.lsl`. The IntelliJ Platform requires Language
 * IDs to be globally unique across loaded plugins; sharing `LSL` throws
 * `ImplementationConflictException` at IDE startup and prevents the IDE
 * from loading at all.
 *
 * The user-visible display name is still "LSL" — it shows up that way
 * in the File Type column, syntax-highlighter settings, etc.
 */
object LslLanguage : Language("sakura-lsl") {
    /** Kept as a public constant so other files don't repeat the literal. */
    const val ID: String = "sakura-lsl"
    private fun readResolve(): Any = LslLanguage
    override fun getDisplayName(): String = "LSL"
    override fun isCaseSensitive(): Boolean = true
}
