package com.attentive.androidsdk;

import androidx.annotation.Nullable;

public enum AttentiveLogLevel {
    VERBOSE(1),
    STANDARD(2),
    LIGHT(3);

    private final int id;

    AttentiveLogLevel(int id) {
        this.id = id;
    }

    @Nullable
    public static AttentiveLogLevel fromId(int logLevelId) {
         AttentiveLogLevel[] values = values();
         for (AttentiveLogLevel value : values) {
             if (value.id == logLevelId) {
                 return value;
             }
         }
         return null;
    }

    int getId() {
        return id;
    }
}
