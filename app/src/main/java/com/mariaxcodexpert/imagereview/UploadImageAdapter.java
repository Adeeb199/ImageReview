package com.mariaxcodexpert.imagereview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        // ===== Glide optimized =====
        Glide.with(holder.img.getContext())
                .load(model.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache images
                .placeholder(R.drawable.error_placeholder)
                .centerCrop()
                .into(holder.img);

        // ===== Uploading overlay =====
        holder.progress.setVisibility(model.isUploading() ? View.VISIBLE : View.GONE);
        holder.overlay.setVisibility(model.isUploading() ? View.VISIBLE : View.GONE);

        // ===== Live review & rating =====
        if (model.getImageId() != null && model.getOwnerUid() != null) {

            DatabaseReference reviewRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(model.getOwnerUid())
                    .child("images")
                    .child(model.getImageId())
                    .child("reviews");

            // Advanced: only fetch once + update model directly
            reviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    model.updateStatsFromFirebase(snapshot);
                    holder.tvReviews.setText("Reviews: " + model.getReviewCount());
                    holder.tvRating.setText("⭐ " + model.getAvgRatingFormatted());
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    holder.tvReviews.setText("Reviews: 0");
                    holder.tvRating.setText("⭐ 0.0");
                }
            });

        } else {
            holder.tvReviews.setText("Reviews: " + model.getReviewCount());
            holder.tvRating.setText("⭐ " + model.getAvgRatingFormatted());
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
