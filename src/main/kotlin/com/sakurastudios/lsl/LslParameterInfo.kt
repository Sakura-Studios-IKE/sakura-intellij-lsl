package com.sakurastudios.lsl

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Shows the signature of an LSL built-in function while the user types
 * its argument list. Without a real PSI parser we work directly off the
 * raw text: find the nearest `name(` left of the caret, look it up in
 * [LslBuiltins.FUNCTIONS], count unescaped commas before the caret to
 * decide which argument is currently being typed.
 *
 * Triggered with `Ctrl+P` and also auto-pops when typing `(` inside
 * an LSL editor (IntelliJ shows it automatically for registered
 * parameter-info handlers).
 */
class LslParameterInfoHandler : ParameterInfoHandler<PsiElement, LslBuiltins.Fn> {

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
        val info = findCallAt(context.file, context.offset) ?: return null
        val fn = LslBuiltins.FUNCTIONS[info.name] ?: return null
        context.itemsToShow = arrayOf<Any>(fn)
        return info.anchorElement
    }

    override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): PsiElement? {
        val info = findCallAt(context.file, context.offset) ?: return null
        return info.anchorElement
    }

    override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
        val info = findCallAt(context.file, context.offset) ?: return
        context.setCurrentParameter(info.activeParamIndex)
    }

    override fun updateUI(p: LslBuiltins.Fn?, context: ParameterInfoUIContext) {
        if (p == null) return
        val parts = p.params
        if (parts.isEmpty()) {
            context.setupUIComponentPresentation(
                "${p.ret} ${p.name}(  )",
                -1, -1, false, false, false,
                context.defaultParameterColor
            )
            return
        }
        val active = context.currentParameterIndex.coerceIn(-1, parts.size - 1)
        val rendered = StringBuilder()
        rendered.append(p.ret).append(' ').append(p.name).append('(')
        var hlStart = -1
        var hlEnd = -1
        for (i in parts.indices) {
            if (i > 0) rendered.append(", ")
            if (i == active) hlStart = rendered.length
            rendered.append(parts[i])
            if (i == active) hlEnd = rendered.length
        }
        rendered.append(')')
        if (p.monoOnly) rendered.append("   (Mono only)")
        context.setupUIComponentPresentation(
            rendered.toString(),
            hlStart, hlEnd,
            false, false, false,
            context.defaultParameterColor
        )
    }
}

/** Result of scanning text for an in-progress function call. */
private data class CallInfo(
    val name: String,
    val anchorElement: PsiElement,
    val activeParamIndex: Int
)

/**
 * Walk the document text backwards from the caret, looking for the
 * function-name + opening paren that opened the call we're inside.
 * Counts unescaped commas at paren depth 1 to know which argument is
 * being edited.
 */
private fun findCallAt(file: PsiFile, offset: Int): CallInfo? {
    val text = file.text
    if (offset <= 0 || offset > text.length) return null
    var depth = 0
    var commas = 0
    var i = offset - 1
    var inString = false
    while (i >= 0) {
        val c = text[i]
        // tracking string state when scanning left is approximate; for
        // our purposes (just deciding which argument we're inside) it's
        // enough to count quotes and skip what comes after.
        if (c == '"' && (i == 0 || text[i - 1] != '\\')) inString = !inString
        if (!inString) {
            when (c) {
                ')' -> depth++
                '(' -> {
                    if (depth == 0) {
                        // Found the opener — scan left for the function name.
                        var j = i - 1
                        while (j >= 0 && (text[j] == ' ' || text[j] == '\t')) j--
                        val end = j + 1
                        while (j >= 0 && (text[j].isLetterOrDigit() || text[j] == '_')) j--
                        val start = j + 1
                        if (start < end) {
                            val name = text.substring(start, end)
                            if (LslBuiltins.FUNCTIONS.containsKey(name)) {
                                val anchor = file.findElementAt(start) ?: return null
                                return CallInfo(name, anchor, commas)
                            }
                        }
                        return null
                    }
                    depth--
                }
                ',' -> if (depth == 0) commas++
                ';', '{', '}' -> return null   // not inside a call argument list
            }
        }
        i--
    }
    return null
}
