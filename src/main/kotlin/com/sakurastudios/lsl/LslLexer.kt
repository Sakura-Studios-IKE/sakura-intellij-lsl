package com.sakurastudios.lsl

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * A small, hand-rolled lexer for LSL.
 *
 * It does not attempt to build a PSI tree — it only produces a stream of
 * IElementType tokens suitable for syntax highlighting, brace matching, and
 * completion-context queries.
 */
class LslLexer : LexerBase() {

    companion object {
        // Reserved words that introduce statements / control flow.
        private val KEYWORDS: Set<String> = setOf(
            "default", "state", "if", "else", "while", "do", "for",
            "return", "jump", "print"
        )

        // LSL primitive type names.
        private val TYPES: Set<String> = setOf(
            "integer", "float", "string", "key",
            "vector", "rotation", "quaternion", "list", "void"
        )

        // Event handler names that occur inside a state body.
        private val EVENTS: Set<String> = setOf(
            "state_entry", "state_exit",
            "touch_start", "touch", "touch_end",
            "collision_start", "collision", "collision_end",
            "land_collision_start", "land_collision", "land_collision_end",
            "timer", "listen", "sensor", "no_sensor",
            "control", "moving_start", "moving_end",
            "money", "email", "at_target", "not_at_target",
            "at_rot_target", "not_at_rot_target",
            "run_time_permissions", "changed", "attach",
            "dataserver", "object_rez", "remote_data",
            "http_response", "http_request",
            "link_message", "on_rez",
            "transaction_result", "experience_permissions",
            "experience_permissions_denied", "path_update",
            "final_damage", "damage", "game_control"
        )

        // Threshold for "is this an ALL_CAPS identifier" → treat as constant.
        private const val MIN_CONST_LEN = 2
    }

    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var bufferIndex = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var currentToken: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.bufferIndex = startOffset
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = currentToken
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        tokenStart = bufferIndex
        if (bufferIndex >= endOffset) {
            currentToken = null
            tokenEnd = bufferIndex
            return
        }
        val c = buffer[bufferIndex]
        currentToken = when {
            c == '\n' || c == '\r'                  -> consumeNewline()
            Character.isWhitespace(c)               -> consumeWhitespace()
            c == '/' && peek(1) == '/'              -> consumeLineComment()
            c == '/' && peek(1) == '*'              -> consumeBlockComment()
            c == '#' && atLineStart()               -> consumeLineComment() // treat #... as preprocessor-comment
            c == '"'                                -> consumeString()
            Character.isJavaIdentifierStart(c)      -> consumeIdentifierOrKeyword()
            Character.isDigit(c)                    -> consumeNumber()
            c == '.' && peek(1)?.let { Character.isDigit(it) } == true -> consumeNumber()
            else                                    -> consumePunctuation()
        }
        tokenEnd = bufferIndex
    }

    // ------------- helpers -------------

    private fun peek(off: Int): Char? {
        val i = bufferIndex + off
        return if (i < endOffset) buffer[i] else null
    }

    private fun atLineStart(): Boolean {
        var i = bufferIndex - 1
        while (i >= startOffset) {
            val ch = buffer[i]
            if (ch == '\n' || ch == '\r') return true
            if (!Character.isWhitespace(ch)) return false
            i--
        }
        return true
    }

    private fun consumeNewline(): IElementType {
        val c = buffer[bufferIndex]
        bufferIndex++
        if (c == '\r' && bufferIndex < endOffset && buffer[bufferIndex] == '\n') bufferIndex++
        return LslTokenTypes.NEWLINE
    }

    private fun consumeWhitespace(): IElementType {
        while (bufferIndex < endOffset) {
            val c = buffer[bufferIndex]
            if (c == '\n' || c == '\r') break
            if (!Character.isWhitespace(c)) break
            bufferIndex++
        }
        return LslTokenTypes.WHITESPACE
    }

    private fun consumeLineComment(): IElementType {
        while (bufferIndex < endOffset && buffer[bufferIndex] != '\n' && buffer[bufferIndex] != '\r') {
            bufferIndex++
        }
        return LslTokenTypes.LINE_COMMENT
    }

    private fun consumeBlockComment(): IElementType {
        bufferIndex += 2 // skip /*
        while (bufferIndex < endOffset) {
            if (buffer[bufferIndex] == '*' && bufferIndex + 1 < endOffset && buffer[bufferIndex + 1] == '/') {
                bufferIndex += 2
                break
            }
            bufferIndex++
        }
        return LslTokenTypes.BLOCK_COMMENT
    }

    private fun consumeString(): IElementType {
        bufferIndex++ // opening "
        while (bufferIndex < endOffset) {
            val c = buffer[bufferIndex]
            if (c == '\\' && bufferIndex + 1 < endOffset) {
                bufferIndex += 2
                continue
            }
            if (c == '"') { bufferIndex++; break }
            if (c == '\n' || c == '\r') break // unterminated
            bufferIndex++
        }
        return LslTokenTypes.STRING_LITERAL
    }

    private fun consumeIdentifierOrKeyword(): IElementType {
        val start = bufferIndex
        bufferIndex++
        while (bufferIndex < endOffset && Character.isJavaIdentifierPart(buffer[bufferIndex])) {
            bufferIndex++
        }
        val text = buffer.subSequence(start, bufferIndex).toString()
        return when {
            text in KEYWORDS                     -> LslTokenTypes.KEYWORD
            text in TYPES                        -> LslTokenTypes.TYPE_KEYWORD
            text in EVENTS                       -> LslTokenTypes.EVENT_KEYWORD
            text.startsWith("ll") && text.length > 2 && text[2].isUpperCase()
                                                 -> LslTokenTypes.LL_IDENTIFIER
            isAllCapsConstant(text)              -> LslTokenTypes.CONSTANT
            else                                 -> LslTokenTypes.IDENTIFIER
        }
    }

    private fun isAllCapsConstant(text: String): Boolean {
        if (text.length < MIN_CONST_LEN) return false
        if (!text[0].isLetter()) return false
        for (ch in text) {
            if (!(ch.isUpperCase() || ch.isDigit() || ch == '_')) return false
        }
        return true
    }

    private fun consumeNumber(): IElementType {
        val start = bufferIndex
        var isFloat = false

        // hex?
        if (buffer[bufferIndex] == '0' && bufferIndex + 1 < endOffset &&
            (buffer[bufferIndex + 1] == 'x' || buffer[bufferIndex + 1] == 'X')) {
            bufferIndex += 2
            while (bufferIndex < endOffset && isHexDigit(buffer[bufferIndex])) bufferIndex++
            return LslTokenTypes.INTEGER_LITERAL
        }

        // leading dot?
        if (buffer[bufferIndex] == '.') {
            isFloat = true
            bufferIndex++
        }
        while (bufferIndex < endOffset && Character.isDigit(buffer[bufferIndex])) bufferIndex++
        if (!isFloat && bufferIndex < endOffset && buffer[bufferIndex] == '.') {
            isFloat = true
            bufferIndex++
            while (bufferIndex < endOffset && Character.isDigit(buffer[bufferIndex])) bufferIndex++
        }
        if (bufferIndex < endOffset && (buffer[bufferIndex] == 'e' || buffer[bufferIndex] == 'E')) {
            isFloat = true
            bufferIndex++
            if (bufferIndex < endOffset && (buffer[bufferIndex] == '+' || buffer[bufferIndex] == '-')) bufferIndex++
            while (bufferIndex < endOffset && Character.isDigit(buffer[bufferIndex])) bufferIndex++
        }
        if (bufferIndex < endOffset && (buffer[bufferIndex] == 'f' || buffer[bufferIndex] == 'F')) {
            isFloat = true
            bufferIndex++
        }
        // safety: ensure we made progress
        if (bufferIndex == start) bufferIndex++
        return if (isFloat) LslTokenTypes.FLOAT_LITERAL else LslTokenTypes.INTEGER_LITERAL
    }

    private fun isHexDigit(c: Char): Boolean =
        c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

    private fun consumePunctuation(): IElementType {
        val c = buffer[bufferIndex]
        bufferIndex++
        return when (c) {
            '{' -> LslTokenTypes.LBRACE
            '}' -> LslTokenTypes.RBRACE
            '(' -> LslTokenTypes.LPAREN
            ')' -> LslTokenTypes.RPAREN
            '[' -> LslTokenTypes.LBRACKET
            ']' -> LslTokenTypes.RBRACKET
            ';' -> LslTokenTypes.SEMI
            ',' -> LslTokenTypes.COMMA
            '.' -> LslTokenTypes.DOT
            '@' -> LslTokenTypes.AT
            '+', '-', '*', '/', '%', '=', '<', '>',
            '!', '&', '|', '^', '~', '?', ':' -> {
                // consume the rest of multi-char operators (==, <=, >=, !=, &&, ||, <<, >>, +=, -=, *=, /=, ++, --)
                if (bufferIndex < endOffset) {
                    val n = buffer[bufferIndex]
                    if (isOperatorContinuation(c, n)) bufferIndex++
                }
                LslTokenTypes.OPERATOR
            }
            else -> LslTokenTypes.BAD_CHARACTER
        }
    }

    private fun isOperatorContinuation(c: Char, n: Char): Boolean {
        return when (c) {
            '=' -> n == '='
            '<' -> n == '=' || n == '<'
            '>' -> n == '=' || n == '>'
            '!' -> n == '='
            '&' -> n == '&' || n == '='
            '|' -> n == '|' || n == '='
            '+' -> n == '+' || n == '='
            '-' -> n == '-' || n == '='
            '*' -> n == '='
            '/' -> n == '='
            '%' -> n == '='
            '^' -> n == '='
            else -> false
        }
    }
}
