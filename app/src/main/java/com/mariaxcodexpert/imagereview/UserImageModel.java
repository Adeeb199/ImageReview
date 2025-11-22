package com.mariaxcodexpert.imagereview;

import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.firebase.database.DataSnapshot;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

// Yeh model sirf Firebase STORAGE + DATABASE ke hisaab se banaya gaya hai.
// Ab Base64 bilkul use nahi ho raha.
// Sirf imageUrl (Firebase Storage ka URL) use hoga.

public class UserImageModel {

    private String imageUrl;   // Firebase Storage URL
    private int reviewCount;   // Total reviews
    private double avgRating;  // Average rating
    private boolean uploading; // True jab tak image upload ho rahi ho
    private String imageId;    // Firebase key (unique per image)
    private String ownerUid;   // User ID jiska image hai

    public UserImageModel() { }

    // Full constructor
    public UserImageModel(String imageUrl, int reviewCount, double avgRating,
                          boolean uploading, String imageId, String ownerUid) {

        this.imageUrl = imageUrl;
        this.reviewCount = reviewCount;
        this.avgRating = avgRating;
        this.uploading = uploading;
        this.imageId = imageId;
        this.ownerUid = ownerUid;
    }

    // =============================
    // GETTERS
    // =============================
    public String getImageUrl() { return imageUrl; }
    public int getReviewCount() { return reviewCount; }
    public double getAvgRating() { return avgRating; }
    public boolean isUploading() { return uploading; }
    public String getImageId() { return imageId; }
    public String getOwnerUid() { return ownerUid; }

    // Rating ko 1 decimal format me wapas do (UI ke liye best)
    public String getAvgRatingFormatted() {
        return String.format("%.1f", avgRating);
    }

    // =============================
    // SETTERS
    // =============================
    public void setImageUrl(String url) { this.imageUrl = url; }
    public void setReviewCount(int count) { this.reviewCount = count; }
    public void setAvgRating(double avg) { this.avgRating = avg; }
    public void setUploading(boolean uploading) { this.uploading = uploading; }
    public void setImageId(String id) { this.imageId = id; }
    public void setOwnerUid(String uid) { this.ownerUid = uid; }

    // =============================
    // Firebase Review Stats Update
    // =============================
    public void updateStatsFromFirebase(DataSnapshot reviewsSnapshot) {

        // Agar reviews nahi mile to default rakho
        if (reviewsSnapshot == null || !reviewsSnapshot.exists()) {
            reviewCount = 0;
            avgRating = 0.0;
            return;
        }

        int totalRating = 0;
        int count = 0;

        for (DataSnapshot snap : reviewsSnapshot.getChildren()) {
            Long rating = snap.child("rating").getValue(Long.class);
            if (rating != null) {
                totalRating += rating;
                count++;
            }
        }

        reviewCount = count;
        avgRating = count > 0 ? (double) totalRating / count : 0.0;
    }

    // Firebase par object upload karne ke liye map me convert karo
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("imageUrl", imageUrl);
        map.put("reviewCount", reviewCount);
        map.put("avgRating", avgRating);
        map.put("uploading", uploading);
        return map;
    }

    public String getAverageRating() {
        // Agar avgRating 0 hai to "0.0" return karo
        if (avgRating == 0) {
            return "0.0";
        }
        // Double ko 1 decimal place me format karo
        return String.format("%.1f", avgRating);
    }

}
