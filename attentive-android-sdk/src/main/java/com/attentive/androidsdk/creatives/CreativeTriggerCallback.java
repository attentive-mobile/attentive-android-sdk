package com.attentive.androidsdk.creatives;

public interface CreativeTriggerCallback {
    default void onOpenFailure(){};

    default void onOpen(){};

    default void onCloseFailure(){};

    default void onClose(){};
}
