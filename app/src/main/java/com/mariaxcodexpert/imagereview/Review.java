package com.mariaxcodexpert.imagereview;

// Review model
public class Review {
    public String reviewerUid;
    public float rating;
    public long timestamp;

    public Review() {} // Required for Firebase

    public Review(String reviewerUid, float rating, long timestamp) {
        this.reviewerUid = reviewerUid;
        this.rating = rating;
        this.timestamp = timestamp;
    }
}