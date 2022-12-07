package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.attentive.androidsdk.events.Purchase;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_page);
    }

    public void purchaseButtonClicked(View view) {
        // Send "Purchase" Event
        Purchase purchase = new Purchase() {{
            setAmount(20.0);
            setProductId("11111");
            // TODO add more properties
        }};
        ((ExampleApp)getApplication()).attentiveEventTracker.recordEvent(purchase);
    }
}