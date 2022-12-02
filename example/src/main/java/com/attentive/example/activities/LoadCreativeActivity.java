package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.creatives.Creative;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;


public class LoadCreativeActivity extends AppCompatActivity {
    private Creative creative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_creative);

        AttentiveConfig attentiveConfig = ((ExampleApp) getApplication()).attentiveConfig;

        // Attach the creative to the provided parentView
        View parentView = (View) findViewById(R.id.loadCreative).getParent();
        this.creative = new Creative(attentiveConfig, parentView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Destroy the creative and it's associated WebView. You must call destroy on the
        // creative when it is no longer in use.
        creative.destroy();
    }

    public void displayCreative(View view) {
        // Clear cookies to avoid creative filtering during testing. Do not clear cookies
        // if you want to test Creative fatigue and filtering
        clearCookies();

        // Display the creative
        creative.trigger();
    }

    private void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }
}