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

import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageHolder> {

    private final List<UserImageModel> list;

    public ImagesAdapter(List<UserImageModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_image, parent, false);
        return new ImageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
        UserImageModel model = list.get(position);

        // Show progress while loading
        holder.progress.setVisibility(View.VISIBLE);
        holder.overlay.setVisibility(View.GONE);

        // Load image from Firebase Storage URL using Glide
        Glide.with(holder.itemView.getContext())
                .load(model.getImageUrl()) // <-- FIXED: use imageUrl instead of Base64
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error_placeholder)
                .into(holder.img);

        // Hide progress after Glide starts loading (optional: can use listener for full control)
        holder.progress.setVisibility(View.GONE);

        // Display review info overlay
        int reviews = model.getReviewCount();
        double avg = model.getAvgRating();

        holder.reviewCount.setText("Reviews: " + reviews);
        holder.avgRating.setText("Avg: " + String.format("%.1f", avg));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addImageAtTop(UserImageModel image) {
        // When new image uploaded, insert at top
        list.add(0, image);
        notifyItemInserted(0);
    }

    static class ImageHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView reviewCount, avgRating;
        ProgressBar progress;
        View overlay;

        public ImageHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgUserImg);
            overlay = itemView.findViewById(R.id.overlayItem);
            progress = itemView.findViewById(R.id.progressItem);
            reviewCount = itemView.findViewById(R.id.tvReviewCount);
            avgRating = itemView.findViewById(R.id.tvAvgRating);
        }
    }
}
