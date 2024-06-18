package com.attentive.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.example.ExampleApp;
import com.attentive.example.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupDomainEditText();
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
        ((ExampleApp) this.getApplication()).getAttentiveConfig().clearUser();
    }

    public void identifyUser(View view) {
        ((ExampleApp) this.getApplication()).getAttentiveConfig().identify(ExampleApp.buildUserIdentifiers());
    }

    private void setupDomainEditText() {
        EditText domainValueTextView = findViewById(R.id.domainValue);
        domainValueTextView.setText(((ExampleApp) this.getApplication()).getAttentiveConfig().getDomain());
        domainValueTextView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ((ExampleApp) getApplication()).getAttentiveConfig().changeDomain(domainValueTextView.getText().toString());
                return true;
            }
            return false;
        });
    }
}
