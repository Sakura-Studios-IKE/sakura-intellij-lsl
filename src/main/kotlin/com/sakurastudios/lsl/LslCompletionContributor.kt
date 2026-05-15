package com.sakurastudios.lsl

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion for LSL identifiers. Offers:
 *   - keywords (`default`, `state`, control flow)
 *   - primitive types
 *   - event handler names
 *   - common built-in `ll*` functions
 *   - common all-caps constants
 *
 * All entries come from [LslBuiltins].
 */
class LslCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(LslLanguage),
            LslCompletionProvider
        )
    }

    private object LslCompletionProvider : CompletionProvider<CompletionParameters>() {

        private const val PRIORITY_BUILTIN_FN = 80.0
        private const val PRIORITY_CONSTANT   = 60.0
        private const val PRIORITY_EVENT      = 50.0
        private const val PRIORITY_TYPE       = 40.0
        private const val PRIORITY_KEYWORD    = 30.0

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            for (fn in LslBuiltins.FUNCTIONS) {
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(fn)
                            .withIcon(LslIcons.FILE)
                            .withTypeText("LSL function", true)
                            .withInsertHandler { ctx, _ ->
                                // Add () and place caret inside.
                                val editor = ctx.editor
                                val offset = editor.caretModel.offset
                                val doc = editor.document
                                doc.insertString(offset, "()")
                                editor.caretModel.moveToOffset(offset + 1)
                            },
                        PRIORITY_BUILTIN_FN
                    )
                )
            }
            for (c in LslBuiltins.CONSTANTS) {
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(c)
                            .withTypeText("LSL constant", true)
                            .bold(),
                        PRIORITY_CONSTANT
                    )
                )
            }
            for (e in LslBuiltins.EVENTS) {
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(e)
                            .withTypeText("LSL event", true),
                        PRIORITY_EVENT
                    )
                )
            }
            for (t in LslBuiltins.TYPES) {
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(t)
                            .withTypeText("type", true),
                        PRIORITY_TYPE
                    )
                )
            }
            for (k in LslBuiltins.KEYWORDS) {
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(k)
                            .withTypeText("keyword", true),
                        PRIORITY_KEYWORD
                    )
                )
            }
        }
    }
}
