package com.sakurastudios.lsl

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls

/** Element type bound to the LSL language. */
class LslElementType(@NonNls debugName: String) : IElementType(debugName, LslLanguage)

/** Singleton holder for every token IElementType the lexer produces. */
object LslTokenTypes {
    @JvmField val WHITESPACE = LslElementType("LSL_WHITESPACE")
    @JvmField val NEWLINE    = LslElementType("LSL_NEWLINE")

    @JvmField val LINE_COMMENT  = LslElementType("LSL_LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = LslElementType("LSL_BLOCK_COMMENT")

    @JvmField val IDENTIFIER    = LslElementType("LSL_IDENTIFIER")
    @JvmField val LL_IDENTIFIER = LslElementType("LSL_LL_IDENTIFIER") // ll-prefixed builtins
    @JvmField val KEYWORD       = LslElementType("LSL_KEYWORD")
    @JvmField val TYPE_KEYWORD  = LslElementType("LSL_TYPE_KEYWORD")
    @JvmField val EVENT_KEYWORD = LslElementType("LSL_EVENT_KEYWORD")
    @JvmField val CONSTANT      = LslElementType("LSL_CONSTANT")

    @JvmField val INTEGER_LITERAL = LslElementType("LSL_INTEGER_LITERAL")
    @JvmField val FLOAT_LITERAL   = LslElementType("LSL_FLOAT_LITERAL")
    @JvmField val STRING_LITERAL  = LslElementType("LSL_STRING_LITERAL")

    @JvmField val PREPROCESSOR = LslElementType("LSL_PREPROCESSOR")

    @JvmField val LBRACE   = LslElementType("LSL_LBRACE")
    @JvmField val RBRACE   = LslElementType("LSL_RBRACE")
    @JvmField val LPAREN   = LslElementType("LSL_LPAREN")
    @JvmField val RPAREN   = LslElementType("LSL_RPAREN")
    @JvmField val LBRACKET = LslElementType("LSL_LBRACKET")
    @JvmField val RBRACKET = LslElementType("LSL_RBRACKET")
    @JvmField val SEMI     = LslElementType("LSL_SEMI")
    @JvmField val COMMA    = LslElementType("LSL_COMMA")
    @JvmField val DOT      = LslElementType("LSL_DOT")
    @JvmField val AT       = LslElementType("LSL_AT")
    @JvmField val OPERATOR = LslElementType("LSL_OPERATOR")

    @JvmField val BAD_CHARACTER = LslElementType("LSL_BAD_CHARACTER")

    /** Tokens treated as comments for IntelliJ's comment handling. */
    @JvmField
    val COMMENTS: TokenSet = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)

    /** Tokens treated as strings. */
    @JvmField
    val STRINGS: TokenSet = TokenSet.create(STRING_LITERAL)
}
