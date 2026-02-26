package com.attentive.androidsdk

import org.junit.Assert
import org.junit.Test

class ParameterValidationTest {
    @Test
    fun verifyNotNull_null_throwsException() {
        Assert.assertThrows(
            IllegalArgumentException::class.java,
        ) { ParameterValidation.verifyNotNull(null, "paramName") }
    }

    @Test
    fun verifyNotNull_notNull_succeeds() {
        ParameterValidation.verifyNotNull("notNull", "paramName")
    }

    @Test
    fun verifyNotEmpty_emptyString_throwsException() {
        Assert.assertThrows(
            IllegalArgumentException::class.java,
        ) { ParameterValidation.verifyNotEmpty("", "paramName") }
    }

    @Test
    fun verifyNotEmpty_notEmptyString_succeeds() {
        ParameterValidation.verifyNotEmpty("notEmpty", "paramName")
    }

    @Test
    fun verifyNotEmpty_emptyCollection_throwsException() {
        Assert.assertThrows(
            IllegalArgumentException::class.java,
        ) {
            ParameterValidation.verifyNotEmpty(
                emptyList<Any>(),
                "paramName",
            )
        }
    }

    @Test
    fun verifyNotEmpty_notEmptyCollection_succeeds() {
        ParameterValidation.verifyNotEmpty(listOf("notEmpty"), "paramName")
    }
}
