package com.attentive.androidsdk;

import static org.junit.Assert.assertThrows;

import java.util.Collection;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;


public class ParameterValidationTest {
    private ParameterValidation parameterValidation;

    @Before
    public void setup() {
        parameterValidation = new ParameterValidation();
    }

    @Test
    public void verifyNotNull_null_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> parameterValidation.verifyNotNull(null, "paramName"));
    }

    @Test
    public void verifyNotNull_notNull_succeeds() {
        parameterValidation.verifyNotNull("notNull", "paramName");
    }

    @Test
    public void verifyNotEmpty_emptyString_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> parameterValidation.verifyNotEmpty("", "paramName"));
    }

    @Test
    public void verifyNotEmpty_nullString_throwsException() {
        String nullString = null;
        assertThrows(IllegalArgumentException.class, () -> parameterValidation.verifyNotEmpty(nullString, "paramName"));
    }

    @Test
    public void verifyNotEmpty_notEmptyString_succeeds() {
        parameterValidation.verifyNotEmpty("notEmpty", "paramName");
    }

    @Test
    public void verifyNotEmpty_emptyCollection_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> parameterValidation.verifyNotEmpty(Collections.emptyList(), "paramName"));
    }

    @Test
    public void verifyNotEmpty_nullCollection_throwsException() {
        Collection nullCollection = null;
        assertThrows(IllegalArgumentException.class, () -> parameterValidation.verifyNotEmpty(nullCollection, "paramName"));
    }

    @Test
    public void verifyNotEmpty_notEmptyCollection_succeeds() {
        parameterValidation.verifyNotEmpty(Collections.singletonList("notEmpty"), "paramName");
    }
}
