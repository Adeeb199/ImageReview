package com.mariaxcodexpert.imagereview;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserProfileLoader {

    /**
     * Loads logged-in user's profile data into the provided views:
     * - Profile Image
     * - Username
     * - Following count
     * - Followers count
     * - Total likes from all reviews on user's images
     *
     * @param context           Context
     * @param ivProfile         ImageView for profile picture
     * @param tvUsername        TextView for username
     * @param tvFollowingCount  TextView for following count
     * @param tvFollowersCount  TextView for followers count
     * @param tvLikesCount      TextView for total likes
     */
    public static void loadUserProfile(
            Context context,
            ImageView ivProfile,
            TextView tvUsername,
            TextView tvFollowingCount,
            TextView tvFollowersCount,
            TextView tvLikesCount
    ) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // --- Load profile info ---
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                tvUsername.setText(name != null ? name : "User");

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(Uri.parse(profileImageUrl))
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .into(ivProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // --- Load following count ---
        dbRef.child("following").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvFollowingCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // --- Load followers count ---
        dbRef.child("followers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvFollowersCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // --- Load total likes from all reviews on all images ---
        dbRef.child("images").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot imagesSnapshot) {
                int totalLikes = 0;

                for (DataSnapshot imageSnap : imagesSnapshot.getChildren()) {
                    if (imageSnap.hasChild("reviews")) {
                        for (DataSnapshot reviewSnap : imageSnap.child("reviews").getChildren()) {
                            Long likes = reviewSnap.child("likes").getValue(Long.class);
                            totalLikes += (likes != null ? likes : 0);
                        }
                    }
                }

                tvLikesCount.setText(String.valueOf(totalLikes));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
