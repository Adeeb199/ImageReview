package com.mariaxcodexpert.imagereview;

import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReviewImageActivity extends AppCompatActivity {

    private static final String TAG = "ReviewImage";

    private ImageView imgReview, btnHeart, btnSmile, btnLike;
    private ProgressBar progressBar;
    private RatingBar ratingBar;
    private EditText etReview;
    private Button btnSubmit, btnSkip;

    private FirebaseUser currentUser;
    private DatabaseReference dbRef;

    private HybridImageSelector imageSelector;
    private final List<String> skippedImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_image);

        try {
            bindUI();
            setupFirebase();
            setupListeners();
            loadImagesFromCacheOrFirebase();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void bindUI() {
        imgReview = findViewById(R.id.imgReview);
        progressBar = findViewById(R.id.progressBar);
        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSkip = findViewById(R.id.btnSkip);

        btnHeart = findViewById(R.id.btnHeart);
        btnSmile = findViewById(R.id.btnSmile);
        btnLike = findViewById(R.id.btnLike);
    }

    private void setupFirebase() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        dbRef = FirebaseDatabase.getInstance("https://imagereview397-default-rtdb.firebaseio.com/")
                .getReference("users");
    }

    private void setupListeners() {
        btnHeart.setOnClickListener(v -> submitReaction("heart", btnHeart));
        btnSmile.setOnClickListener(v -> submitReaction("smile", btnSmile));
        btnLike.setOnClickListener(v -> submitReaction("like", btnLike));

        btnSubmit.setOnClickListener(v -> submitReview());
        btnSkip.setOnClickListener(v -> skipImage());
    }

    private void loadImagesFromCacheOrFirebase() {
        try {
            List<HybridImageSelector.ImageData> cached = ImageCache.getInstance().getImages();
            if (!cached.isEmpty()) {
                imageSelector = new HybridImageSelector(cached, skippedImages, currentUser.getUid(), 30);
                displayCurrentImage();
            } else {
                loadImagesFromFirebase();
            }
        } catch (Exception e) {
            Log.e(TAG, "loadImagesFromCacheOrFirebase error", e);
            Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadImagesFromFirebase() {
        try {
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        List<HybridImageSelector.ImageData> pool = new ArrayList<>();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String uid = userSnapshot.getKey();
                            if (uid.equals(currentUser.getUid())) continue;
                            if (!userSnapshot.hasChild("images")) continue;

                            for (DataSnapshot imageSnap : userSnapshot.child("images").getChildren()) {
                                String imageEncoded = imageSnap.child("image").getValue(String.class);
                                if (imageEncoded == null || imageEncoded.isEmpty()) continue;

                                int reviewCount = (int) imageSnap.child("reviews").getChildrenCount();
                                boolean alreadyReviewed = imageSnap.hasChild("reviews") &&
                                        imageSnap.child("reviews").hasChild(currentUser.getUid());

                                Boolean skipped = null;
                                if (alreadyReviewed)
                                    skipped = imageSnap.child("reviews").child(currentUser.getUid())
                                            .child("skipped").getValue(Boolean.class);

                                if ((skipped != null && skipped) || alreadyReviewed) continue;

                                pool.add(new HybridImageSelector.ImageData(
                                        uid, imageSnap.getKey(), imageEncoded, reviewCount, false));
                            }
                        }

                        if (pool.isEmpty()) {
                            Toast.makeText(ReviewImageActivity.this, "No images to review", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        imageSelector = new HybridImageSelector(pool, skippedImages, currentUser.getUid(), 30);
                        displayCurrentImage();
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase snapshot", e);
                        Toast.makeText(ReviewImageActivity.this, "Failed to process images", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase load cancelled", error.toException());
                    Toast.makeText(ReviewImageActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "loadImagesFromFirebase error", e);
            Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayCurrentImage() {
        try {
            HybridImageSelector.ImageData imageData = imageSelector.peekNextImage();
            if (imageData == null) {
                Toast.makeText(this, "All images reviewed", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            progressBar.setVisibility(android.view.View.VISIBLE);
            imgReview.setVisibility(android.view.View.INVISIBLE);
            setButtonsEnabled(false);

            btnHeart.setAlpha(0.4f);
            btnSmile.setAlpha(0.4f);
            btnLike.setAlpha(0.4f);
            ratingBar.setRating(0);
            etReview.setText("");

            String base64Uri = "data:image/png;base64," + imageData.imageBase64.replaceAll("\\s+", "");
            Glide.with(this)
                    .load(base64Uri)
                    .centerCrop()
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(android.view.View.GONE);
                            Toast.makeText(ReviewImageActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            setButtonsEnabled(false);
                            Log.e(TAG, "Glide load failed", e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(android.view.View.GONE);
                            imgReview.setVisibility(android.view.View.VISIBLE);
                            fadeIn(imgReview);
                            setButtonsEnabled(true);
                            return false;
                        }
                    })
                    .into(imgReview);

        } catch (Exception e) {
            Log.e(TAG, "displayCurrentImage error", e);
            progressBar.setVisibility(android.view.View.GONE);
            setButtonsEnabled(false);
            Toast.makeText(this, "Error displaying image", Toast.LENGTH_SHORT).show();
        }
    }

    private void fadeIn(android.view.View view) {
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(300);
        view.startAnimation(fadeIn);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnSubmit.setEnabled(enabled);
        btnSkip.setEnabled(enabled);
        btnHeart.setEnabled(enabled);
        btnSmile.setEnabled(enabled);
        btnLike.setEnabled(enabled);
        float alpha = enabled ? 1f : 0.4f;
        btnSubmit.setAlpha(alpha);
        btnSkip.setAlpha(alpha);
        btnHeart.setAlpha(enabled && btnHeart.getAlpha() == 1f ? 1f : 0.4f);
        btnSmile.setAlpha(enabled && btnSmile.getAlpha() == 1f ? 1f : 0.4f);
        btnLike.setAlpha(enabled && btnLike.getAlpha() == 1f ? 1f : 0.4f);
    }

    private void safeFirebaseWrite(DatabaseReference ref, Object value, Runnable onSuccess) {
        try {
            setButtonsEnabled(false);
            ref.setValue(value)
                    .addOnSuccessListener(aVoid -> {
                        try {
                            onSuccess.run();
                        } catch (Exception e) {
                            Log.e(TAG, "onSuccess Runnable error", e);
                        } finally {
                            setButtonsEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase write failed", e);
                        Toast.makeText(this, "Action failed, try again", Toast.LENGTH_SHORT).show();
                        setButtonsEnabled(true);
                    });
        } catch (Exception e) {
            Log.e(TAG, "safeFirebaseWrite error", e);
            Toast.makeText(this, "Unexpected error during action", Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        }
    }

    private void nextImage() {
        try {
            HybridImageSelector.ImageData currentImage = imageSelector.peekNextImage();
            if (currentImage != null) imageSelector.markImageInteracted(currentImage);
            displayCurrentImage();
        } catch (Exception e) {
            Log.e(TAG, "nextImage error", e);
            displayCurrentImage();
        }
    }

    private void submitReaction(String reaction, ImageView clickedBtn) {
        try {
            HybridImageSelector.ImageData imageData = imageSelector.peekNextImage();
            if (imageData == null) return;

            safeFirebaseWrite(dbRef.child(imageData.uid)
                            .child("images").child(imageData.imageId)
                            .child("reviews").child(currentUser.getUid())
                            .child("reaction"),
                    reaction, () -> {
                        imageSelector.markImageInteracted(imageData);
                        highlightSelectedEmoji(clickedBtn);
                        nextImage();
                    });

        } catch (Exception e) {
            Log.e(TAG, "submitReaction error", e);
            Toast.makeText(this, "Failed to submit reaction", Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        }
    }

    private void submitReview() {
        try {
            HybridImageSelector.ImageData imageData = imageSelector.peekNextImage();
            if (imageData == null) return;

            float rating = ratingBar.getRating();
            String reviewText = etReview.getText().toString();

            safeFirebaseWrite(dbRef.child(imageData.uid)
                            .child("images").child(imageData.imageId)
                            .child("reviews").child(currentUser.getUid())
                            .child("rating"),
                    (int) rating, () -> {});

            safeFirebaseWrite(dbRef.child(imageData.uid)
                            .child("images").child(imageData.imageId)
                            .child("reviews").child(currentUser.getUid())
                            .child("text"),
                    reviewText, () -> {
                        imageSelector.markImageInteracted(imageData);
                        nextImage();
                    });

        } catch (Exception e) {
            Log.e(TAG, "submitReview error", e);
            Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        }
    }

    private void skipImage() {
        try {
            HybridImageSelector.ImageData imageData = imageSelector.peekNextImage();
            if (imageData == null) return;

            skippedImages.add(imageData.imageId);
            imageSelector.skipImage(imageData);

            safeFirebaseWrite(dbRef.child(imageData.uid)
                            .child("images").child(imageData.imageId)
                            .child("reviews").child(currentUser.getUid())
                            .child("skipped"),
                    true, this::nextImage);

        } catch (Exception e) {
            Log.e(TAG, "skipImage error", e);
            Toast.makeText(this, "Failed to skip image", Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        }
    }

    private void highlightSelectedEmoji(ImageView selected) {
        btnHeart.setAlpha(selected == btnHeart ? 1f : 0.4f);
        btnSmile.setAlpha(selected == btnSmile ? 1f : 0.4f);
        btnLike.setAlpha(selected == btnLike ? 1f : 0.4f);
    }
}
