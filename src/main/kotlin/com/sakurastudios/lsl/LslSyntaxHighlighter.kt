package com.sakurastudios.lsl

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

/**
 * Maps LSL token types to IntelliJ TextAttributesKeys.
 *
 * Keys are defined in terms of the platform-default highlight colours so they
 * inherit user theme tweaks (Darcula, light, high-contrast).
 */
class LslSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = LslLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            LslTokenTypes.KEYWORD, LslTokenTypes.EVENT_KEYWORD -> KEYWORD_KEYS
            LslTokenTypes.TYPE_KEYWORD     -> TYPE_KEYS
            LslTokenTypes.LL_IDENTIFIER    -> BUILTIN_KEYS
            LslTokenTypes.CONSTANT         -> CONSTANT_KEYS
            LslTokenTypes.IDENTIFIER       -> IDENTIFIER_KEYS
            LslTokenTypes.STRING_LITERAL   -> STRING_KEYS
            LslTokenTypes.INTEGER_LITERAL,
            LslTokenTypes.FLOAT_LITERAL    -> NUMBER_KEYS
            LslTokenTypes.LINE_COMMENT     -> LINE_COMMENT_KEYS
            LslTokenTypes.BLOCK_COMMENT    -> BLOCK_COMMENT_KEYS
            LslTokenTypes.OPERATOR, LslTokenTypes.AT, LslTokenTypes.DOT -> OPERATOR_KEYS
            LslTokenTypes.LBRACE, LslTokenTypes.RBRACE -> BRACES_KEYS
            LslTokenTypes.LPAREN, LslTokenTypes.RPAREN -> PARENS_KEYS
            LslTokenTypes.LBRACKET, LslTokenTypes.RBRACKET -> BRACKETS_KEYS
            LslTokenTypes.SEMI             -> SEMI_KEYS
            LslTokenTypes.COMMA            -> COMMA_KEYS
            LslTokenTypes.PREPROCESSOR     -> LINE_COMMENT_KEYS
            LslTokenTypes.BAD_CHARACTER    -> BAD_CHAR_KEYS
            else                           -> EMPTY_KEYS
        }

    companion object {
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "LSL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE = TextAttributesKey.createTextAttributesKey(
            "LSL_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)
        val BUILTIN = TextAttributesKey.createTextAttributesKey(
            "LSL_BUILTIN_FUNCTION", DefaultLanguageHighlighterColors.STATIC_METHOD)
        val CONSTANT = TextAttributesKey.createTextAttributesKey(
            "LSL_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT)
        val IDENTIFIER = TextAttributesKey.createTextAttributesKey(
            "LSL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val STRING = TextAttributesKey.createTextAttributesKey(
            "LSL_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = TextAttributesKey.createTextAttributesKey(
            "LSL_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
            "LSL_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey(
            "LSL_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val OPERATOR = TextAttributesKey.createTextAttributesKey(
            "LSL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val BRACES = TextAttributesKey.createTextAttributesKey(
            "LSL_BRACES", DefaultLanguageHighlighterColors.BRACES)
        val PARENS = TextAttributesKey.createTextAttributesKey(
            "LSL_PARENS", DefaultLanguageHighlighterColors.PARENTHESES)
        val BRACKETS = TextAttributesKey.createTextAttributesKey(
            "LSL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val SEMI = TextAttributesKey.createTextAttributesKey(
            "LSL_SEMI", DefaultLanguageHighlighterColors.SEMICOLON)
        val COMMA = TextAttributesKey.createTextAttributesKey(
            "LSL_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val BAD_CHAR = TextAttributesKey.createTextAttributesKey(
            "LSL_BAD_CHARACTER", com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER)

        private val KEYWORD_KEYS         = arrayOf(KEYWORD)
        private val TYPE_KEYS            = arrayOf(TYPE)
        private val BUILTIN_KEYS         = arrayOf(BUILTIN)
        private val CONSTANT_KEYS        = arrayOf(CONSTANT)
        private val IDENTIFIER_KEYS      = arrayOf(IDENTIFIER)
        private val STRING_KEYS          = arrayOf(STRING)
        private val NUMBER_KEYS          = arrayOf(NUMBER)
        private val LINE_COMMENT_KEYS    = arrayOf(LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS   = arrayOf(BLOCK_COMMENT)
        private val OPERATOR_KEYS        = arrayOf(OPERATOR)
        private val BRACES_KEYS          = arrayOf(BRACES)
        private val PARENS_KEYS          = arrayOf(PARENS)
        private val BRACKETS_KEYS        = arrayOf(BRACKETS)
        private val SEMI_KEYS            = arrayOf(SEMI)
        private val COMMA_KEYS           = arrayOf(COMMA)
        private val BAD_CHAR_KEYS        = arrayOf(BAD_CHAR)
        private val EMPTY_KEYS: Array<TextAttributesKey> = emptyArray()
    }
}

/** Factory that hands an LslSyntaxHighlighter to IntelliJ. */
class LslSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        LslSyntaxHighlighter()
}
