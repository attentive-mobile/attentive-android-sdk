package com.attentive.androidsdk;

import android.content.Context;

public class ClassFactory {
    public static PersistentStorage buildPersistentStorage(Context context) {
        return new PersistentStorage(context);
    }

    public static VisitorService buildVisitorService(PersistentStorage persistentStorage) {
        return new VisitorService(persistentStorage);
    }
}
