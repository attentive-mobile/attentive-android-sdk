package com.attentive.androidsdk

enum class AttentiveLogLevel(val id: Int) {
    VERBOSE(1),
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
