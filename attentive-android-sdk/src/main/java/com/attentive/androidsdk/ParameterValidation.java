package com.attentive.androidsdk;

import android.util.Log;
import java.util.Collection;

// TODO move to 'internal' package
public class ParameterValidation {
    public static boolean verifyNotNull(Object param, String paramName) {
        if (param == null) {
            Log.e(ParameterValidation.class.getName(), paramName + " cannot be null.");
            return false;
        }
        return true;
    }

    public static boolean verifyNotEmpty(String param, String paramName) {
        if (verifyNotNull(param, paramName) && param.isEmpty()) {
            Log.e(ParameterValidation.class.getName(), paramName + " cannot be empty.");
            return false;
        }
        return true;
    }

    public static <T> boolean verifyNotEmpty(Collection<T> param, String paramName) {
        if (verifyNotNull(param, paramName) && param.isEmpty()) {
            Log.e(ParameterValidation.class.getName(), paramName + " cannot be empty.");
            return false;
        }
        return true;
    }
}
