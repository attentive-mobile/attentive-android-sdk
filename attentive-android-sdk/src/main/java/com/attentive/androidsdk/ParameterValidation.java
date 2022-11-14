package com.attentive.androidsdk;

class ParameterValidation {
    public static void verifyNotNull(Object param, String paramName) {
        if (param == null) {
            throw new IllegalArgumentException(paramName + " cannot be null.");
        }
    }
    public static void verifyNotEmpty(String param, String paramName) {
        verifyNotNull(param, paramName);

        if (param.isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be empty.");
        }
    }
}
