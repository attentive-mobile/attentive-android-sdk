package com.attentive.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.androidsdk.AttentiveConfig;
import com.attentive.androidsdk.AttentiveLogLevel;
import com.attentive.androidsdk.creatives.Creative;
import com.attentive.androidsdk.creatives.CreativeTriggerCallback;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;

public class LoadCreativeActivity extends AppCompatActivity {
    private Creative creative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_creative);

        // Configuration for testing
        runTestSetup();

        // Attach the creative to the provided parentView
        View parentView = (View) findViewById(R.id.loadCreative).getParent();
        this.creative = new Creative(((ExampleApp) getApplication()).getAttentiveConfig(), parentView, this);
        Button loadCreativeButton = findViewById(R.id.creativeButton);
        loadCreativeButton.setOnClickListener(this::displayCreative);
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

        // Display the creative, with a callback handler
        // You can also call creative.trigger() without a callback handler
        creative.trigger(new CreativeTriggerCallback() {
            @Override
            public void onCreativeNotOpened() {
                Log.e(this.getClass().getName(), "Couldn't open the creative!");
            }

            @Override
            public void onOpen() {
                Log.i(this.getClass().getName(), "Opened the creative!");
            }

            @Override
            public void onCreativeNotClosed() {
                Log.e(this.getClass().getName(), "Couldn't close the creative!");
            }

            @Override
            public void onClose() {
                Log.i(this.getClass().getName(), "Closed the creative!");
            }
        }, null);
    }

    @Override
    public void onBackPressed() {
        boolean creativeClosed = creative.onBackPressed();
        if (!creativeClosed) {
            super.onBackPressed();
        }
    }

    private void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }

    // Method for setting up UI Tests. Only used for testing purposes
    private void runTestSetup() {
        Intent intent = getIntent();
        String domain = intent.getStringExtra("DOMAIN");
        String mode = intent.getStringExtra("MODE");
        boolean skipFatigue = intent.getBooleanExtra("SKIP_FATIGUE", false);

        ((ExampleApp) getApplication()).getAttentiveConfig().clearUser();

        if (domain != null && mode != null) {
            AttentiveConfig config = new AttentiveConfig.Builder()
                    .context(getApplicationContext())
                    .domain(domain)
                    .mode(AttentiveConfig.Mode.valueOf(mode))
                    .skipFatigueOnCreatives(skipFatigue)
                    .logLevel(AttentiveLogLevel.VERBOSE)
                    .build();
            ((ExampleApp) getApplication()).setAttentiveConfig(config);
        }
    }
}