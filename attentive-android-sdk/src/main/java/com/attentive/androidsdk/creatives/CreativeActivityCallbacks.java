package com.attentive.androidsdk.creatives;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import org.jetbrains.annotations.NotNull;

@RestrictTo(RestrictTo.Scope.LIBRARY)
class CreativeActivityCallbacks implements Application.ActivityLifecycleCallbacks {

    @Nullable
    private Creative creative;

    public CreativeActivityCallbacks(@NotNull Creative creative) {
        this.creative = creative;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // No-op
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // No-op
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // No-op
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // No-op
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // No-op
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // No-op
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (creative != null) {
            creative.destroy();
        }
        creative = null;
    }
}