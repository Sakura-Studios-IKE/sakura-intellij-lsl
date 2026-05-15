package com.sakurastudios.lsl

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Hover- and Ctrl+Q-driven docs. Without a full PSI we can't reliably
 * resolve to a PsiElement, so we lean on the surrounding text: extract
 * the word at the caret and look it up in [LslBuiltins]. Returns a small
 * HTML snippet with the symbol's category, plus a link to the LSL Wiki
 * which has the canonical signature + semantics.
 */
class LslDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? = contextElement

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val word = (element ?: originalElement)?.text?.trim().orEmpty()
        if (word.isEmpty()) return null
        return docFor(word)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? =
        generateDoc(element, originalElement)
}

/** Pure function so tests can hit it without the IntelliJ runtime. */
fun docFor(word: String): String? {
    return when {
        LslBuiltins.isFunction(word) -> buildString {
            append("<b>$word</b> &mdash; LSL built-in function<br>")
            append("Part of the documented LSL Mono library.<br><br>")
            append("See: <a href=\"https://wiki.secondlife.com/wiki/$word\">LSL Wiki — $word</a>")
        }
        LslBuiltins.isConstant(word) -> buildString {
            append("<b>$word</b> &mdash; LSL built-in constant<br>")
            append("See: <a href=\"https://wiki.secondlife.com/wiki/$word\">LSL Wiki — $word</a>")
        }
        LslBuiltins.isEvent(word) -> buildString {
            append("<b>$word</b> &mdash; LSL event handler<br>")
            append("Place inside a <code>default { ... }</code> or named <code>state X { ... }</code> block.<br><br>")
            append("See: <a href=\"https://wiki.secondlife.com/wiki/$word\">LSL Wiki — $word</a>")
        }
        LslBuiltins.isType(word) -> "<b>$word</b> &mdash; LSL primitive type"
        LslBuiltins.isKeyword(word) -> "<b>$word</b> &mdash; LSL keyword"
        LslBuiltins.looksLikeBuiltinFunction(word) ->
            "<b>$word</b> &mdash; likely an LSL built-in (not in our local catalogue; check the LSL Wiki)"
        else -> null
    }
}
