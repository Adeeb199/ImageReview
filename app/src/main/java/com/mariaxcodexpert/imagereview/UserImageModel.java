package com.mariaxcodexpert.imagereview;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for user images.
 * Includes Base64 image, review count, average rating, and upload state.
 * Now supports real-time updates from Firebase snapshot.
 */
public class UserImageModel {

    private String base64Image;
    private int reviewCount;
    private double avgRating;
    private boolean isUploading; // true if image is currently uploading
    private String imageId;      // Firebase image key
    private String ownerUid;     // User ID of the image owner
    private String id;

    /**
     * Constructor for existing images loaded from Firebase.
     *
     * @param base64Image Base64 encoded image
     * @param reviewCount Number of reviews (default 0)
     * @param avgRating   Average rating (default 0.0)
     */
    public UserImageModel(String base64Image, int reviewCount, double avgRating) {
        this(base64Image, reviewCount, avgRating, false, null, null);
    }

    /**
     * Full constructor including upload state and Firebase info.
     *
     * @param base64Image Base64 encoded image
     * @param reviewCount Number of reviews
     * @param avgRating   Average rating
     * @param isUploading True if image is currently uploading
     * @param imageId     Firebase image key
     * @param ownerUid    Owner UID
     */
    public UserImageModel(String base64Image, int reviewCount, double avgRating,
                          boolean isUploading, String imageId, String ownerUid) {
        this.base64Image = base64Image;
        this.reviewCount = reviewCount;
        this.avgRating = avgRating;
        this.isUploading = isUploading;
        this.imageId = imageId;
        this.ownerUid = ownerUid;
    }
    // Inside UserImageModel.java
    public UserImageModel(String base64Image, int reviewCount, double avgRating, boolean isUploading) {
        this(base64Image, reviewCount, avgRating, isUploading, null, null);
    }

    public UserImageModel(String base64, int reviewCount, double avgRating, boolean uploading, String id) {
        this.base64Image = base64;
        this.reviewCount = reviewCount;
        this.avgRating = avgRating;
        this.isUploading = uploading;
        this.id = id;
    }

    // =====================
    // Getters
    // =====================
    public String getBase64Image() {
        return base64Image;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public boolean isUploading() {
        return isUploading;
    }

    public String getImageId() {
        return imageId;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public String getId() { return id; }

    /**
     * Returns the Base64-encoded image string.
     *
     * @return Base64 string of the image
     */
    public String getBase64() {
        return base64Image;
    }

    // =====================
    // Setters
    // =====================
    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public void setUploading(boolean uploading) {
        isUploading = uploading;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    // =====================
    // Firebase Utilities
    // =====================

    /**
     * Updates reviewCount and avgRating from a Firebase snapshot of "reviews".
     *
     * @param reviewsSnapshot Firebase DataSnapshot pointing to image reviews
     */
    public void updateStatsFromFirebase(DataSnapshot reviewsSnapshot) {
        if (reviewsSnapshot == null || !reviewsSnapshot.exists()) {
            reviewCount = 0;
            avgRating = 0.0;
            return;
        }

        int totalRating = 0;
        int count = 0;

        for (DataSnapshot reviewSnap : reviewsSnapshot.getChildren()) {
            Long rating = reviewSnap.child("rating").getValue(Long.class);
            if (rating != null) {
                totalRating += rating.intValue();
                count++;
            }
        }

        reviewCount = count;
        avgRating = count > 0 ? (double) totalRating / count : 0.0;
    }

    /**
     * Converts the object to a Firebase-compatible Map for upload.
     *
     * @return Map<String, Object>
     */
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("image", base64Image);
        map.put("reviewCount", reviewCount);
        map.put("avgRating", avgRating);
        map.put("isUploading", isUploading);
        return map;
    }
}
