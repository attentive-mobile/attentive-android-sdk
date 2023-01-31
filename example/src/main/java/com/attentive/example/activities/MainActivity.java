package com.attentive.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView domainValueTextView = findViewById(R.id.domainValue);
        domainValueTextView.setText(((ExampleApp)this.getApplication()).attentiveConfig.getDomain());
    }

    public void startLoadCreativeActivity(View view) {
        Intent intent = new Intent(this, LoadCreativeActivity.class);
        startActivity(intent);
    }

    public void startProductPageActivity(View view) {
        Intent intent = new Intent(this, ProductPageActivity.class);
        startActivity(intent);
    }

    public void logoutUser(View view) {
        // Perform app's normal logout functionality

        // Clear all Attentive identifiers
        ((ExampleApp)this.getApplication()).attentiveConfig.clearUser();
    }

    public void identifyUser(View view) {
        ((ExampleApp)this.getApplication()).attentiveConfig.identify(ExampleApp.buildUserIdentifiers());
    }
}