package com.attentive.androidsdk.creatives;

public interface CreativeTriggerCallback {
    // Called when the Creative has been triggered but it is not opened successfully.
    // This can happen if there is no available mobile app creative, if the creative is fatigued,
    // if the creative call has been timed out, or if an unknown exception occurs.
    default void onCreativeNotOpened(){};

    // Called when the Creative is opened successfully
    default void onOpen(){};

    // Called when the creative is not closed successfully due to an unknown exception
    default void onCreativeNotClosed(){};

    // Called when the Creative is closed successfully
    default void onClose(){};
}
