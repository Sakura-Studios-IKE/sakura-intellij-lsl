package com.sakurastudios.lsl

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * Highlights the matching brace / bracket / angle in pairs while the
 * caret is on either side. LSL uses:
 *
 *   { }   block, state body
 *   ( )   call args, conditions
 *   [ ]   list literal
 *   < >   vector / rotation literal — but also less-than/greater-than;
 *         marking them as a structural pair is still useful and the
 *         lexer's TK_LANGLE/TK_RANGLE tokens disambiguate.
 */
class LslBraceMatcher : PairedBraceMatcher {

    private val pairs = arrayOf(
        BracePair(LslTokenTypes.LBRACE,   LslTokenTypes.RBRACE,   true),
        BracePair(LslTokenTypes.LPAREN,   LslTokenTypes.RPAREN,   false),
        BracePair(LslTokenTypes.LBRACKET, LslTokenTypes.RBRACKET, false)
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(
        lbraceType: IElementType,
        contextType: IElementType?
    ): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int =
        openingBraceOffset
}
