package com.sakurastudios.lsl

/**
 * Static catalogue of the most common LSL built-in functions, constants, and
 * event handlers used by the completion contributor.
 *
 * The list is intentionally a representative subset (~60 functions, ~40
 * constants, all event names) rather than the full ~430-function table that
 * `sakura-lslc` knows about; the full table will be bundled as
 * `builtins.txt` in a future release.
 */
object LslBuiltins {

    @JvmField
    val FUNCTIONS: List<String> = listOf(
        // Avatar / object / world info
        "llGetOwner", "llGetKey", "llGetObjectName", "llSetObjectName",
        "llGetPos", "llSetPos", "llGetRot", "llSetRot",
        "llGetScale", "llSetScale", "llGetVel", "llGetTime",
        "llResetTime", "llGetUnixTime", "llGetRegionName",
        // Math / conversion
        "llAbs", "llFabs", "llCeil", "llFloor", "llRound",
        "llSqrt", "llPow", "llCos", "llSin", "llAtan2",
        "llFrand", "llVecMag", "llVecNorm", "llVecDist",
        // List helpers
        "llGetListLength", "llList2String", "llList2Integer",
        "llList2Float", "llList2Vector", "llList2Key", "llList2List",
        "llCSV2List", "llList2CSV", "llListInsertList",
        "llListReplaceList", "llListSort", "llParseString2List",
        "llDumpList2String",
        // String helpers
        "llStringLength", "llGetSubString", "llSubStringIndex",
        "llToLower", "llToUpper", "llStringTrim", "llEscapeURL",
        "llUnescapeURL", "llBase64ToString", "llStringToBase64",
        // Comms
        "llSay", "llShout", "llWhisper", "llOwnerSay",
        "llRegionSay", "llRegionSayTo", "llInstantMessage",
        "llDialog", "llListen", "llListenRemove",
        // Timers / sensor
        "llSetTimerEvent", "llSensor", "llSensorRepeat", "llSensorRemove",
        // Inventory / scripts
        "llResetScript", "llGetInventoryName", "llGetInventoryNumber",
        "llGiveInventory", "llRemoveInventory",
        // HTTP
        "llHTTPRequest", "llHTTPResponse",
        // Misc utility
        "llDie", "llSleep", "llRequestPermissions", "llTakeControls",
        "llReleaseControls"
    )

    @JvmField
    val CONSTANTS: List<String> = listOf(
        "TRUE", "FALSE", "NULL_KEY", "ZERO_VECTOR", "ZERO_ROTATION",
        "PI", "TWO_PI", "PI_BY_TWO", "DEG_TO_RAD", "RAD_TO_DEG",
        "ALL_SIDES", "AGENT", "ACTIVE", "PASSIVE", "SCRIPTED",
        "OBJECT_NAME", "OBJECT_DESC", "OBJECT_POS", "OBJECT_OWNER",
        "OBJECT_CREATOR", "OBJECT_GROUP", "OBJECT_VELOCITY",
        "CHANGED_INVENTORY", "CHANGED_OWNER", "CHANGED_REGION",
        "CHANGED_TELEPORT", "CHANGED_REGION_START", "CHANGED_LINK",
        "CHANGED_SHAPE", "CHANGED_COLOR", "CHANGED_TEXTURE",
        "PERMISSION_TAKE_CONTROLS", "PERMISSION_TRIGGER_ANIMATION",
        "PERMISSION_ATTACH", "PERMISSION_DEBIT", "PERMISSION_TRACK_CAMERA",
        "PUBLIC_CHANNEL", "DEBUG_CHANNEL",
        "INVENTORY_ALL", "INVENTORY_SCRIPT", "INVENTORY_NOTECARD",
        "INVENTORY_TEXTURE", "INVENTORY_SOUND", "INVENTORY_OBJECT",
        "STATUS_PHYSICS", "STATUS_PHANTOM", "STATUS_ROTATE_X",
        "STATUS_ROTATE_Y", "STATUS_ROTATE_Z",
        "HTTP_METHOD", "HTTP_MIMETYPE", "HTTP_BODY_MAXLENGTH",
        "HTTP_VERIFY_CERT", "HTTP_VERBOSE_THROTTLE"
    )

    /** Event handler names (used for completion inside `state` blocks). */
    @JvmField
    val EVENTS: List<String> = listOf(
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
        "experience_permissions_denied", "path_update"
    )

    @JvmField
    val TYPES: List<String> = listOf(
        "integer", "float", "string", "key", "vector",
        "rotation", "quaternion", "list"
    )

    @JvmField
    val KEYWORDS: List<String> = listOf(
        "default", "state", "if", "else", "while", "do",
        "for", "return", "jump", "print"
    )
}
