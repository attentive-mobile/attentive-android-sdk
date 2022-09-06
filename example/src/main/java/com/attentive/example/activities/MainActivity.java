package com.attentive.example.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.attentive.example.R;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startLoadCreativeActivity(View view) {
        Intent intent = new Intent(this, LoadCreativeActivity.class);
        startActivity(intent);
    }
}