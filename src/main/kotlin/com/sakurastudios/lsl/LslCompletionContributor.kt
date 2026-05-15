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
            for ((name, fn) in LslBuiltins.FUNCTIONS) {
                val tail = "(${fn.params.joinToString(", ")})"
                val ret = fn.ret + (if (fn.monoOnly) "  (Mono)" else "")
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(name)
                            .withIcon(LslIcons.FILE)
                            .withTailText(tail, true)
                            .withTypeText(ret, true)
                            .withPresentableText(name)
                            .withInsertHandler { ctx, _ ->
                                val editor = ctx.editor
                                val offset = editor.caretModel.offset
                                val doc = editor.document
                                doc.insertString(offset, "()")
                                // park the caret inside the parens when the
                                // function takes args; after them when zero-arity
                                editor.caretModel.moveToOffset(offset + if (fn.params.isEmpty()) 2 else 1)
                            },
                        PRIORITY_BUILTIN_FN
                    )
                )
            }
            for ((name, c) in LslBuiltins.CONSTANTS) {
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(name)
                            .withTailText(if (c.value.isNotEmpty()) " = ${c.value}" else "", true)
                            .withTypeText(c.type, true)
                            .bold(),
                        PRIORITY_CONSTANT
                    )
                )
            }
            for ((name, e) in LslBuiltins.EVENTS) {
                val tail = "(${e.params.joinToString(", ") { "${it.type} ${it.name}" }})"
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(name)
                            .withTailText(tail, true)
                            .withTypeText("event", true),
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
