package com.sakurastudios.lsl

import com.intellij.lang.Commenter

/**
 * Wires `Ctrl+/` (line) and `Ctrl+Shift+/` (block) inside .lsl/.lslh files.
 *
 * LSL uses the same syntax as C: `// line` and the block form
 * starting with slash-star and ending with star-slash.
 */
class LslCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String = "/*"
    override fun getBlockCommentSuffix(): String = "*/"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
