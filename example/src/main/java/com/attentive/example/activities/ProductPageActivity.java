package com.attentive.example.activities;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.attentive.androidsdk.AttentiveEventTracker;
import com.attentive.androidsdk.Product;
import com.attentive.androidsdk.ProductView;
import com.attentive.androidsdk.Purchase;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;
import java.util.List;

public class ProductPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_page);

        // Send "Product View" Event
        ProductView productView = new ProductView() {{
            setProducts(List.of(new Product() {{
                setName("Awesome Sweatshirt");
                setProductId("11111");
            }}));
        }};
        AttentiveEventTracker.getInstance().recordEvent(productView);
    }

    public void purchaseButtonClicked(View view) {
        // Send "Purchase" Event
        Purchase purchase = new Purchase() {{
            setAmount(20.0);
            setProductId("11111");
        }};
        AttentiveEventTracker.getInstance().recordEvent(purchase);
    }
}