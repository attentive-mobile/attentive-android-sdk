package com.attentive.androidsdk;

import java.util.Collection;
import android.util.Log;

// TODO move to 'internal' package
public class ParameterValidation {
    private static final String TAG = "ParameterValidation";
    public static void verifyNotNull(Object param, String paramName) {
        if (param == null) {
            Log.e(TAG, paramName + " cannot be null.");
        }
    }

    public static void verifyNotEmpty(String param, String paramName) {
        verifyNotNull(param, paramName);

        if (param.isEmpty()) {
            Log.e(TAG, paramName + " cannot be empty.");
        }
    }

    public static <T> void verifyNotEmpty(Collection<T> param, String paramName) {
        verifyNotNull(param, paramName);

        if (param.isEmpty()) {
            Log.e(TAG, paramName + " cannot be empty.");
        }
    }
}
