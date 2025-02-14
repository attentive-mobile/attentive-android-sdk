package com.attentive.androidsdk

// TODO move to 'internal' package
object ParameterValidation {
    @JvmStatic
    fun verifyNotNull(param: Any?, paramName: String) {
        requireNotNull(param) { "$paramName cannot be null." }
    }

    @JvmStatic
    fun verifyNotEmpty(param: String, paramName: String) {
        verifyNotNull(param, paramName)

        require(param.isNotEmpty()) { "$paramName cannot be empty." }
    }

    @JvmStatic
    fun verifyNotEmpty(param: Collection<*>, paramName: String) {
        verifyNotNull(param, paramName)

        require(!param.isEmpty()) { "$paramName cannot be empty." }
    }
}
