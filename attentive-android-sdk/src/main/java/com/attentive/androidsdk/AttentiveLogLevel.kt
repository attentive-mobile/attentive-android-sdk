package com.attentive.androidsdk

/**
 * Log level for the Attentive SDK's Timber tree.
 *
 * @property id Stable numeric identifier, used for persisting the log level in
 *   SharedPreferences.
 */
enum class AttentiveLogLevel(val id: Int) {
    /** All SDK logs, including debug and verbose messages. */
    VERBOSE(1),

    /** Default log level — info, warnings, and errors only. */
    STANDARD(2),
    ;

    companion object {
        fun fromId(logLevelId: Int): AttentiveLogLevel? {
            val values = entries.toTypedArray()
            for (value in values) {
                if (value.id == logLevelId) {
                    return value
                }
            }
            return null
        }
    }
}
