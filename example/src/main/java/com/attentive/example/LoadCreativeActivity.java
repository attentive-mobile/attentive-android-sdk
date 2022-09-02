package com.attentive.example;

import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.mobilesdk.AttentiveConfig;
import com.attentive.mobilesdk.creatives.Creative;


public class LoadCreativeActivity extends AppCompatActivity {
    private static final String APP_USER_ID = "olivia115";
    private final AttentiveConfig attentiveConfig = new AttentiveConfig(
            "pocket7games", AttentiveConfig.Mode.PRODUCTION);
    private Creative creative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_creative);

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
        creative.trigger(APP_USER_ID);
    }

    private void clearCookies() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
    }
}