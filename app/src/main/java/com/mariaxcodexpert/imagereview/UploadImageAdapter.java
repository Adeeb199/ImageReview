package com.mariaxcodexpert.imagereview;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class UploadImageAdapter extends RecyclerView.Adapter<UploadImageAdapter.ImageHolder> {

    private final ArrayList<UserImageModel> list;

    public UploadImageAdapter(ArrayList<UserImageModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_image, parent, false);
        return new ImageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageHolder holder, int position) {

        UserImageModel model = list.get(position);

        // Decode Base64 and load image
        byte[] decoded = Base64.decode(model.getBase64().replaceAll("\\s+", ""), Base64.DEFAULT);
        Glide.with(holder.img.getContext())
                .asBitmap()
                .load(decoded)
                .placeholder(R.drawable.placeholder)
                .into(holder.img);

        // Show uploading state
        if (model.isUploading()) {
            holder.progress.setVisibility(View.VISIBLE);
            holder.overlay.setVisibility(View.VISIBLE);
        } else {
            holder.progress.setVisibility(View.GONE);
            holder.overlay.setVisibility(View.GONE);
        }

        // Load live review & rating from Firebase
        if (model.getImageId() != null && model.getOwnerUid() != null) {
            DatabaseReference reviewRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(model.getOwnerUid())
                    .child("images")
                    .child(model.getImageId())
                    .child("reviews");

            reviewRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    model.updateStatsFromFirebase(snapshot);
                    holder.tvReviews.setText("Reviews: " + model.getReviewCount());
                    holder.tvRating.setText("⭐ " + model.getAverageRating());
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    holder.tvReviews.setText("Reviews: 0");
                    holder.tvRating.setText("⭐ 0.0");
                }
            });
        } else {
            holder.tvReviews.setText("Reviews: " + model.getReviewCount());
            holder.tvRating.setText("⭐ " + model.getAverageRating());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ImageHolder extends RecyclerView.ViewHolder {

        ImageView img;
        TextView tvReviews, tvRating;
        View overlay;
        ProgressBar progress;

        public ImageHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgUserImg);
            tvReviews = itemView.findViewById(R.id.tvReviewCount);
            tvRating = itemView.findViewById(R.id.tvAvgRating);
            overlay = itemView.findViewById(R.id.overlayItem);
            progress = itemView.findViewById(R.id.progressItem);
        }
    }
}
