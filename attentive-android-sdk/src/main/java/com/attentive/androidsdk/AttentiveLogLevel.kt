package com.attentive.androidsdk

/**
 * Log level for the Attentive SDK's Timber tree.
 *
 * @property id A stable numeric identifier, used for persisting the log level in SharedPreferences.
 */
enum class AttentiveLogLevel(val id: Int) {
    /** All SDK logs, including debug and verbose messages. */
    VERBOSE(1),

    /** Default log level — info, warnings, and errors only. */
    STANDARD(2),
    ;

    companion object {
        /**
         * Looks up an [AttentiveLogLevel] by its persistent [id].
         *
         * @param logLevelId The [id] value previously returned from this enum.
         * @return The matching [AttentiveLogLevel], or `null` if no level matches [logLevelId].
         */
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
