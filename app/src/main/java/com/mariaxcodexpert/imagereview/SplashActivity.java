package com.mariaxcodexpert.imagereview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);

        // Check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // User already logged in → go to MainActivity
            progressBar.setVisibility(ProgressBar.VISIBLE);
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        } else {
            // User not logged in → wait 2 seconds then go to LoginActivity
            progressBar.setVisibility(ProgressBar.VISIBLE);
            progressBar.postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }, 2000);
        }
    }
}
