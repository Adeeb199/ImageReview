package com.mariaxcodexpert.imagereview;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView imgUser;
    private MaterialTextView txtUserName, txtUserEmail;
    private LinearLayout cardUploadImage, cardReviewImage;
    private MaterialTextView btnLogout;

    private final int MIN_PRELOAD_COUNT = 30;
    private FirebaseUser currentUser;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        imgUser = findViewById(R.id.imgUser);
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        cardUploadImage = findViewById(R.id.cardUploadImage);
        cardReviewImage = findViewById(R.id.cardReviewImage);
        btnLogout = findViewById(R.id.btnLogout);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        setupUserProfile();

        dbRef = FirebaseDatabase.getInstance("https://imagereview397-default-rtdb.firebaseio.com/")
                .getReference("users");

        preloadImagesToCache();

        // Card click listeners
        cardUploadImage.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, UploadImageActivity.class))
        );

        cardReviewImage.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReviewImageActivity.class))
        );

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            redirectToLogin();
        });
    }

    private void setupUserProfile() {
        String displayName = currentUser.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                displayName = currentUser.getEmail().split("@")[0];
            } else {
                displayName = "User";
            }
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();
            currentUser.updateProfile(profileUpdates);
        }

        txtUserName.setText(displayName);
        txtUserEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this).load(currentUser.getPhotoUrl()).circleCrop().into(imgUser);
        } else {
            imgUser.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void preloadImagesToCache() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ReviewImageActivity.ImageData> preloadedImages = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    if (uid.equals(currentUser.getUid())) continue;
                    if (!userSnapshot.hasChild("images")) continue;

                    for (DataSnapshot imageSnap : userSnapshot.child("images").getChildren()) {
                        String imageEncoded = imageSnap.child("image").getValue(String.class);
                        if (imageEncoded == null || imageEncoded.isEmpty()) continue;

                        boolean alreadyReviewed = imageSnap.hasChild("reviews") &&
                                imageSnap.child("reviews").hasChild(currentUser.getUid());

                        Boolean skipped = null;
                        if (alreadyReviewed)
                            skipped = imageSnap.child("reviews").child(currentUser.getUid())
                                    .child("skipped").getValue(Boolean.class);

                        if ((skipped != null && skipped) || alreadyReviewed) continue;

                        String imageId = imageSnap.getKey();
                        preloadedImages.add(new ReviewImageActivity.ImageData(
                                uid, imageId, imageEncoded
                        ));

                        if (preloadedImages.size() >= MIN_PRELOAD_COUNT) break;
                    }
                    if (preloadedImages.size() >= MIN_PRELOAD_COUNT) break;
                }

                if (!preloadedImages.isEmpty()) {
                    ImageCache.getInstance().setImages(preloadedImages);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silent fail
            }
        });
    }
}
