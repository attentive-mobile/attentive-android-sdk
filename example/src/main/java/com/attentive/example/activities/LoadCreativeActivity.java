package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;
import com.attentive.mobilesdk.AttentiveConfig;
import com.attentive.mobilesdk.creatives.Creative;


public class LoadCreativeActivity extends AppCompatActivity {
    private Creative creative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_creative);

        AttentiveConfig attentiveConfig = ((ExampleApp) getApplication()).attentiveConfig;

        // Register the current user with the Attentive SDK. Replace "APP_USER_ID"
        // with the current user's ID. You must register a user ID before calling
        // `trigger` on a Creative.
        attentiveConfig.identify("APP_USER_ID");

        View parentView = (View) findViewById(R.id.loadCreative).getParent();
        this.creative = new Creative(attentiveConfig, parentView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        creative.destroy();
    }

    public void displayCreative(View view) {
        // Clear cookies to avoid creative filtering
        clearCookies();
        creative.trigger();
    }

    private void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }
}