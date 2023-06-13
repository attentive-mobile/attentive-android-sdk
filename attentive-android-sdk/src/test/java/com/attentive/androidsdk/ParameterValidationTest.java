package com.attentive.androidsdk;

import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

public class ParameterValidationTest {
    @Test
    public void verifyNotNull_null_doesntThrowException() {
        ParameterValidation.verifyNotNull(null, "paramName");
    }

    @Test
    public void verifyNotNull_notNull_succeeds() {
        ParameterValidation.verifyNotNull("notNull", "paramName");
    }

    @Test
    public void verifyNotEmpty_emptyString_doesntThrowException() {
        ParameterValidation.verifyNotEmpty("", "paramName");
    }

    @Test
    public void verifyNotEmpty_nullString_doesntThrowException() {
        String nullString = null;
        ParameterValidation.verifyNotEmpty(nullString, "paramName");
    }

    @Test
    public void verifyNotEmpty_notEmptyString_succeeds() {
        ParameterValidation.verifyNotEmpty("notEmpty", "paramName");
    }

    @Test
    public void verifyNotEmpty_emptyCollection_doesntThrowException() {
        ParameterValidation.verifyNotEmpty(Collections.emptyList(), "paramName");
    }

    @Test
    public void verifyNotEmpty_nullCollection_doesntThrowException() {
        Collection<?> nullCollection = null;
        ParameterValidation.verifyNotEmpty(nullCollection, "paramName");
    }

    @Test
    public void verifyNotEmpty_notEmptyCollection_succeeds() {
        ParameterValidation.verifyNotEmpty(Collections.singletonList("notEmpty"), "paramName");
    }
}
