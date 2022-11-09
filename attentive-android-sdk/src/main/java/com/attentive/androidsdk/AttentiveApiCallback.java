package com.attentive.androidsdk;

public interface AttentiveApiCallback {
    void onFailure(String message);

    void onSuccess();
}
