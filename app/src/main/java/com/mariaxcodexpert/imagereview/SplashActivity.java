package com.mariaxcodexpert.imagereview;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imgLogo = findViewById(R.id.imgLogo);
        progressBar = findViewById(R.id.progressBar);

        // -------------------------------
        // 1️⃣ Logo Animation: Fade + Bounce
        // -------------------------------
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(imgLogo, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(imgLogo, "scaleY", 0.5f, 1f);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(fadeIn, scaleX, scaleY);
        logoAnim.setDuration(1200);
        logoAnim.setInterpolator(new OvershootInterpolator()); // bounce effect
        logoAnim.start();

        // -------------------------------
        // 2️⃣ ProgressBar: Continuous rotation
        // -------------------------------
        ObjectAnimator rotate = ObjectAnimator.ofFloat(progressBar, "rotation", 0f, 360f);
        rotate.setDuration(1000);
        rotate.setRepeatCount(ObjectAnimator.INFINITE);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.start();

        // -------------------------------
        // 3️⃣ Optional: Shimmer effect on Logo (red shimmer)
        // -------------------------------
        imgLogo.post(() -> {
            ValueAnimator shimmer = ValueAnimator.ofFloat(0f, 1f);
            shimmer.setDuration(1500);
            shimmer.setRepeatCount(ValueAnimator.INFINITE);
            shimmer.setRepeatMode(ValueAnimator.REVERSE);
            shimmer.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                imgLogo.setAlpha(0.7f + 0.3f * value); // shimmer by changing alpha
            });
            shimmer.start();
        });

        // -------------------------------
        // 4️⃣ Firebase login check
        // -------------------------------
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        int splashDelay = 2500; // 2.5 seconds splash

        new Handler().postDelayed(() -> {
            if (user != null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, splashDelay);
    }
}
