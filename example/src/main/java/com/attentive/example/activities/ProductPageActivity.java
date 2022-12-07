package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.attentive.androidsdk.AttentiveAnalytics;
import com.attentive.androidsdk.Product;
import com.attentive.androidsdk.ProductView;
import com.attentive.androidsdk.Purchase;
import com.attentive.example.R;
import java.util.List;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_page);
    }

    public void sendPurchaseEvent(View view) {
        // TODO
    }
}