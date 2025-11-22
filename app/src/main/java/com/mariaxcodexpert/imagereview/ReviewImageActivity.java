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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewImageActivity extends AppCompatActivity {

    private static final String TAG = "ReviewImage";

    private ImageView imgReview, btnHeart, btnSmile, btnLike;
    private ProgressBar progressBar;
    private RatingBar ratingBar;
    private EditText etReview;
    private Button btnSubmit, btnSkip;

    private FirebaseUser currentUser;
    private DatabaseReference dbRef;

    private final List<ImageData> imageQueue = new ArrayList<>();
    private final List<String> skippedImages = new ArrayList<>();
    private int currentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_image);

        bindUI();
        setupFirebase();
        setupListeners();
        loadImagesFromCacheOrFirebase();
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
        List<ImageData> cached = ImageCache.getInstance().getImages();
        if (!cached.isEmpty()) {
            imageQueue.addAll(cached);
            displayNextImage();
        } else {
            loadImagesFromFirebase();
        }
    }

    private void loadImagesFromFirebase() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ImageData> preloadedImages = new ArrayList<>();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    if (uid.equals(currentUser.getUid())) continue;
                    if (!userSnap.hasChild("images")) continue;

                    for (DataSnapshot imgSnap : userSnap.child("images").getChildren()) {
                        String imageUrl = imgSnap.child("imageUrl").getValue(String.class);
                        if (imageUrl == null || imageUrl.isEmpty()) continue;

                        boolean alreadyReviewed = imgSnap.hasChild("reviews") &&
                                imgSnap.child("reviews").hasChild(currentUser.getUid());
                        Boolean skipped = null;
                        if (alreadyReviewed)
                            skipped = imgSnap.child("reviews").child(currentUser.getUid())
                                    .child("skipped").getValue(Boolean.class);

                        if ((skipped != null && skipped) || alreadyReviewed) continue;

                        preloadedImages.add(new ImageData(uid, imgSnap.getKey(), imageUrl));
                    }
                }

                if (preloadedImages.isEmpty()) {
                    Toast.makeText(ReviewImageActivity.this, "No images to review", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                imageQueue.addAll(preloadedImages);
                ImageCache.getInstance().setImages(preloadedImages); // cache for next time
                displayNextImage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase load cancelled", error.toException());
                Toast.makeText(ReviewImageActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayNextImage() {
        currentIndex++;
        if (currentIndex >= imageQueue.size()) {
            Toast.makeText(this, "All images reviewed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageData current = imageQueue.get(currentIndex);

        progressBar.setVisibility(android.view.View.VISIBLE);
        imgReview.setVisibility(android.view.View.INVISIBLE);
        setButtonsEnabled(false);

        btnHeart.setAlpha(0.4f);
        btnSmile.setAlpha(0.4f);
        btnLike.setAlpha(0.4f);
        ratingBar.setRating(0);
        etReview.setText("");

        Glide.with(this)
                .load(current.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(ReviewImageActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(android.view.View.GONE);
                        imgReview.setVisibility(android.view.View.VISIBLE);
                        fadeIn(imgReview);
                        setButtonsEnabled(true);

                        // Preload next image
                        if (currentIndex + 1 < imageQueue.size()) {
                            String nextUrl = imageQueue.get(currentIndex + 1).imageUrl;
                            Glide.with(ReviewImageActivity.this)
                                    .load(nextUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload();
                        }
                        return false;
                    }
                })
                .into(imgReview);
    }

    private void fadeIn(android.view.View view) {
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(250);
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
    }

    private void safeFirebaseWrite(DatabaseReference ref, Object value, Runnable onSuccess) {
        setButtonsEnabled(false);
        ref.setValue(value)
                .addOnSuccessListener(aVoid -> {
                    onSuccess.run();
                    setButtonsEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase write failed", e);
                    Toast.makeText(this, "Action failed, try again", Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                });
    }

    private void submitReaction(String reaction, ImageView clickedBtn) {
        if (currentIndex >= imageQueue.size()) return;
        ImageData current = imageQueue.get(currentIndex);

        safeFirebaseWrite(dbRef.child(current.uid)
                        .child("images").child(current.imageId)
                        .child("reviews").child(currentUser.getUid())
                        .child("reaction"),
                reaction, this::displayNextImage);

        highlightSelectedEmoji(clickedBtn);
    }

    private void submitReview() {
        if (currentIndex >= imageQueue.size()) return;
        ImageData current = imageQueue.get(currentIndex);

        float rating = ratingBar.getRating();
        String reviewText = etReview.getText().toString();

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("rating", (int) rating);
        reviewData.put("text", reviewText);

        safeFirebaseWrite(dbRef.child(current.uid)
                        .child("images").child(current.imageId)
                        .child("reviews").child(currentUser.getUid()),
                reviewData, this::displayNextImage);
    }

    private void skipImage() {
        if (currentIndex >= imageQueue.size()) return;
        ImageData current = imageQueue.get(currentIndex);
        skippedImages.add(current.imageId);

        safeFirebaseWrite(dbRef.child(current.uid)
                        .child("images").child(current.imageId)
                        .child("reviews").child(currentUser.getUid())
                        .child("skipped"),
                true, this::displayNextImage);
    }

    private void highlightSelectedEmoji(ImageView selected) {
        btnHeart.setAlpha(selected == btnHeart ? 1f : 0.4f);
        btnSmile.setAlpha(selected == btnSmile ? 1f : 0.4f);
        btnLike.setAlpha(selected == btnLike ? 1f : 0.4f);
    }

    // --------------------- Inner class for image data ---------------------
    static class ImageData {
        String uid;
        String imageId;
        String imageUrl; // Use Firebase Storage URL now

        ImageData(String uid, String imageId, String imageUrl) {
            this.uid = uid;
            this.imageId = imageId;
            this.imageUrl = imageUrl;
        }
    }
}
