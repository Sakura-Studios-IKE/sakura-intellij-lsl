package com.sakurastudios.lsl

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Catalogue of every LSL built-in function, constant, and event handler
 * that the completion contributor and documentation provider expose.
 *
 * Loaded once at class-init time from the resource
 * `/builtins/lsl-builtins.txt`, which is the verbatim output of
 * `sakura-lslc --list-builtins`. Regenerate by running
 *
 *     ./sakura-lslc/lslc --list-builtins > \
 *         sakura-intellij-lsl/src/main/resources/builtins/lsl-builtins.txt
 *
 * Format of the resource (line-oriented):
 *
 *     # 540 functions
 *     llGetOwner
 *     llGetCreator
 *     ...
 *     # 534 constants
 *     TRUE
 *     FALSE
 *     ...
 *     # 49 events
 *     state_entry
 *     ...
 */
object LslBuiltins {

    @JvmField
    val FUNCTIONS: List<String>

    @JvmField
    val CONSTANTS: List<String>

    @JvmField
    val EVENTS: List<String>

    /** The LSL reserved-word set used by the lexer/highlighter. */
    @JvmField
    val KEYWORDS: List<String> = listOf(
        "default", "state", "if", "else", "while", "do", "for",
        "return", "jump", "print"
    )

    /** LSL primitive types (also lexed as keywords). */
    @JvmField
    val TYPES: List<String> = listOf(
        "integer", "float", "string", "key",
        "vector", "rotation", "quaternion", "list"
    )

    init {
        // Default fallbacks if the resource is missing for any reason —
        // never leaves the user with zero completion.
        val functions = mutableListOf<String>()
        val constants = mutableListOf<String>()
        val events = mutableListOf<String>()
        var bucket: MutableList<String>? = null

        try {
            val stream = LslBuiltins::class.java.getResourceAsStream("/builtins/lsl-builtins.txt")
            if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { r ->
                    r.lineSequence().forEach { raw ->
                        val line = raw.trim()
                        if (line.isEmpty()) return@forEach
                        if (line.startsWith("#")) {
                            bucket = when {
                                line.contains("functions") -> functions
                                line.contains("constants") -> constants
                                line.contains("events")    -> events
                                else                       -> null
                            }
                            return@forEach
                        }
                        bucket?.add(line)
                    }
                }
            }
        } catch (_: Exception) {
            // fall through to defaults; FUNCTIONS/CONSTANTS will just be sparse
        }

        // Minimal safety net so completion isn't completely empty if the
        // resource was somehow unreadable.
        if (functions.isEmpty()) functions.addAll(listOf(
            "llSay", "llOwnerSay", "llWhisper", "llShout",
            "llGetOwner", "llGetKey", "llGetObjectName", "llSetObjectName",
            "llListen", "llListenRemove", "llDialog", "llTextBox",
            "llSetText", "llHTTPRequest", "llRequestPermissions",
            "llGiveMoney", "llSleep", "llSetTimerEvent", "llResetScript", "llDie"
        ))
        if (constants.isEmpty()) constants.addAll(listOf(
            "TRUE", "FALSE", "NULL_KEY", "EOF", "PI", "TWO_PI", "PI_BY_TWO",
            "PERMISSION_DEBIT", "CHANGED_OWNER", "HTTP_METHOD"
        ))
        if (events.isEmpty()) events.addAll(listOf(
            "state_entry", "state_exit", "touch_start", "touch", "touch_end",
            "listen", "timer", "money", "http_response", "changed",
            "on_rez", "attach", "run_time_permissions", "link_message"
        ))

        FUNCTIONS = functions.toList()
        CONSTANTS = constants.toList()
        EVENTS    = events.toList()
    }

    /** Quick predicate for `ll*` recognition without scanning the full list. */
    @JvmStatic
    fun looksLikeBuiltinFunction(name: String): Boolean =
        name.length >= 3 && name[0] == 'l' && name[1] == 'l' && name[2].isUpperCase()

    @JvmStatic
    fun isFunction(name: String): Boolean = FUNCTIONS.contains(name)
    @JvmStatic
    fun isConstant(name: String): Boolean = CONSTANTS.contains(name)
    @JvmStatic
    fun isEvent(name: String): Boolean    = EVENTS.contains(name)
    @JvmStatic
    fun isType(name: String): Boolean     = TYPES.contains(name)
    @JvmStatic
    fun isKeyword(name: String): Boolean  = KEYWORDS.contains(name)
}
