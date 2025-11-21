package com.mariaxcodexpert.imagereview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

        // Reset overlay and progress
        holder.overlay.setVisibility(View.GONE);
        holder.progress.setVisibility(View.GONE);

        // Decode Base64 to Bitmap and load with Glide
        try {
            byte[] decode = Base64.decode(model.getBase64Image(), Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(decode, 0, decode.length);
            Glide.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(bmp)
                    .centerCrop()
                    .into(holder.img);
        } catch (Exception ignored) {}

        // Display review info overlay
        // Ensure default 0 values if null
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
        // When new image uploaded, insert at top with default review=0, avg=0
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
