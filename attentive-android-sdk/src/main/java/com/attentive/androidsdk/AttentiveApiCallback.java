package com.attentive.androidsdk;

interface AttentiveApiCallback {
    void onFailure(String message);

    void onSuccess();
}
