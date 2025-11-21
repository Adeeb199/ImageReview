package com.mariaxcodexpert.imagereview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HybridAlgorithm with Sliding Window:
 * 1️⃣ Excludes user's own images
 * 2️⃣ Excludes already reviewed/skipped images (both locally & Firebase)
 * 3️⃣ Prioritizes least-reviewed images
 * 4️⃣ Randomizes selection among equal-priority images
 * 5️⃣ Maintains a local batch of images (default 30) for smooth UX
 */
public class HybridImageSelector {

    private static final int DEFAULT_WINDOW_SIZE = 30;

    private final List<ImageData> imagePool;      // All loaded images
    private final List<String> skippedImages;     // Image IDs skipped/interacted by user
    private final String currentUserId;
    private final int windowSize;

    private int currentIndex = 0; // pointer to current image

    public HybridImageSelector(List<ImageData> initialPool, List<String> skippedImages, String currentUserId) {
        this(initialPool, skippedImages, currentUserId, DEFAULT_WINDOW_SIZE);
    }

    public HybridImageSelector(List<ImageData> initialPool, List<String> skippedImages, String currentUserId, int windowSize) {
        this.imagePool = new ArrayList<>(initialPool);
        this.skippedImages = new ArrayList<>(skippedImages);
        this.currentUserId = currentUserId;
        this.windowSize = windowSize;
        sortPoolByReviewCount();
    }

    /**
     * Sort pool by review count ascending
     */
    private void sortPoolByReviewCount() {
        Collections.sort(imagePool, Comparator.comparingInt(img -> img.reviewCount));
    }

    /**
     * Returns the next image based on hybrid rules and moves pointer forward
     */
    public ImageData getNextImage() {
        ImageData next = peekNextImage();
        if (next != null) currentIndex++;
        return next;
    }

    /**
     * Returns the next image without advancing the pointer
     */
    public ImageData peekNextImage() {
        List<ImageData> validImages = new ArrayList<>();
        for (int i = currentIndex; i < imagePool.size(); i++) {
            ImageData img = imagePool.get(i);
            if (!img.uid.equals(currentUserId)
                    && !skippedImages.contains(img.imageId)
                    && !img.alreadyReviewed) {
                validImages.add(img);
            }
            if (validImages.size() >= windowSize) break;
        }

        if (validImages.isEmpty()) return null;

        int minReviews = validImages.get(0).reviewCount;
        List<ImageData> leastReviewed = new ArrayList<>();
        for (ImageData img : validImages) {
            if (img.reviewCount == minReviews) leastReviewed.add(img);
            else break;
        }

        Collections.shuffle(leastReviewed);
        return leastReviewed.get(0);
    }

    /**
     * Mark an image as interacted by the user (reviewed, reacted, or skipped)
     * This ensures it will not appear again in the session
     */
    public void markImageInteracted(ImageData image) {
        if (image != null) {
            image.alreadyReviewed = true;       // local flag
            if (!skippedImages.contains(image.imageId)) {
                skippedImages.add(image.imageId);
            }
        }
    }

    /**
     * Skip an image manually
     */
    public void skipImage(ImageData image) {
        markImageInteracted(image); // treat skip same as interaction
    }

    /**
     * Add new images to the pool (e.g., loaded from Firebase)
     */
    public void addImages(List<ImageData> newImages) {
        for (ImageData img : newImages) {
            if (!imagePool.contains(img) && !skippedImages.contains(img.imageId)) {
                imagePool.add(img);
            }
        }
        // Keep only the most recent windowSize images
        if (imagePool.size() > windowSize) {
            imagePool.subList(0, imagePool.size() - windowSize).clear();
            if (currentIndex > imagePool.size()) currentIndex = imagePool.size() - 1;
        }
        sortPoolByReviewCount();
    }

    public int getPoolSize() {
        return imagePool.size();
    }

    /**
     * ImageData model for this algorithm
     */
    public static class ImageData {
        public String uid;
        public String imageId;
        public String imageBase64;
        public int reviewCount;
        public boolean alreadyReviewed; // true if user interacted

        public ImageData(String uid, String imageId, String imageBase64, int reviewCount, boolean alreadyReviewed) {
            this.uid = uid;
            this.imageId = imageId;
            this.imageBase64 = imageBase64;
            this.reviewCount = reviewCount;
            this.alreadyReviewed = alreadyReviewed;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ImageData)) return false;
            return this.imageId.equals(((ImageData) obj).imageId);
        }

        @Override
        public int hashCode() {
            return imageId.hashCode();
        }
    }
}
