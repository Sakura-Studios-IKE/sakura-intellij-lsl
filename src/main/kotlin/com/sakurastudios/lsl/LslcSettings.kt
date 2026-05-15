package com.sakurastudios.lsl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level persistent settings for the Sakura LSL plugin.
 *
 * Stored as `sakura-lsl.xml` under the IDE configuration directory.
 */
@State(
    name = "com.sakurastudios.lsl.LslcSettings",
    storages = [Storage("sakura-lsl.xml")]
)
class LslcSettings : PersistentStateComponent<LslcSettings> {

    /** Path or command name for `sakura-lslc`. */
    @JvmField var lslcPath: String = "lslc"

    /** Path or command name for `sakura-slemu`. */
    @JvmField var slemuPath: String = "slemu"

    /** Path or command name for `sakura-lsltest`. */
    @JvmField var lslTestPath: String = "lsltest"

    /** Directory Firestorm has been configured to watch for external edits. */
    @JvmField var firestormWatchDir: String = ""

    /** Extra flags passed to lslc on every invocation. */
    @JvmField var extraLslcFlags: String = ""

    override fun getState(): LslcSettings = this

    override fun loadState(state: LslcSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): LslcSettings =
            ApplicationManager.getApplication().getService(LslcSettings::class.java)
    }
}
