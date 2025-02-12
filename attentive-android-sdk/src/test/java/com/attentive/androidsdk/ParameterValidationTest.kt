package com.attentive.androidsdk

import com.attentive.androidsdk.ParameterValidation
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ParameterValidationTest {

    @Test
    fun verifyNotNull_null_throwsException() {
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { ParameterValidation.verifyNotNull(null, "paramName") }
    }

    @Test
    fun verifyNotNull_notNull_succeeds() {
        ParameterValidation.verifyNotNull("notNull", "paramName")
    }

    @Test
    fun verifyNotEmpty_emptyString_throwsException() {
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { ParameterValidation.verifyNotEmpty("", "paramName") }
    }

    @Test
    fun verifyNotEmpty_nullString_throwsException() {
        val nullString: String? = null
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { ParameterValidation.verifyNotEmpty(nullString!!, "paramName") }
    }

    @Test
    fun verifyNotEmpty_notEmptyString_succeeds() {
        ParameterValidation.verifyNotEmpty("notEmpty", "paramName")
    }

    @Test
    fun verifyNotEmpty_emptyCollection_throwsException() {
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) {
            ParameterValidation.verifyNotEmpty(
                emptyList<Any>(),
                "paramName"
            )
        }
    }

    @Test
    fun verifyNotEmpty_nullCollection_throwsException() {
        val nullCollection: Collection<*>? = null
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { ParameterValidation.verifyNotEmpty(nullCollection!!, "paramName") }
    }

    @Test
    fun verifyNotEmpty_notEmptyCollection_succeeds() {
        ParameterValidation.verifyNotEmpty(listOf("notEmpty"), "paramName")
    }
}
