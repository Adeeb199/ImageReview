package com.mariaxcodexpert.imagereview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class UploadImageActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final String TAG = "UploadImageActivity";

    private ImageView btnAddImage;
    private RecyclerView rvImages;
    private TextView tvNoImages;
    private ProgressBar progressBar;
    private ImagesAdapter adapter;
    private ArrayList<UserImageModel> imageList = new ArrayList<>();
    private FirebaseUser currentUser;
    private DatabaseReference dbRef;

    // In-memory cache for fast loading
    private final HashMap<String, Bitmap> imageCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        btnAddImage = findViewById(R.id.btnAddImage);
        rvImages = findViewById(R.id.rvImages);
        tvNoImages = findViewById(R.id.tvNoImages);
        progressBar = findViewById(R.id.progressBar);

        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ImagesAdapter(imageList);
        rvImages.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance("https://imagereview397-default-rtdb.firebaseio.com/")
                .getReference("users");

        btnAddImage.setOnClickListener(v -> openFileChooser());

        loadUserImagesInfo();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            addImageToFeed(data.getData());
        }
    }

    // New image upload (top of feed)
    private void addImageToFeed(Uri imageUri) {
        if (imageUri == null) return;

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            String imageId = String.valueOf(System.currentTimeMillis());

            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            imageCache.put(imageId, bitmap); // cache

            UserImageModel tempImage = new UserImageModel(encodedImage, 0, 0.0, true, null, null);

            imageList.add(0, tempImage); // top
            adapter.notifyItemInserted(0);
            rvImages.scrollToPosition(0);

            progressBar.setVisibility(View.VISIBLE);

            DatabaseReference imageRef = dbRef.child(currentUser.getUid()).child("images").child(imageId);
            imageRef.child("image").setValue(encodedImage);
            imageRef.child("timestamp").setValue(System.currentTimeMillis())
                    .addOnCompleteListener(task -> runOnUiThread(() -> {
                        tempImage.setUploading(false);
                        adapter.notifyItemChanged(0);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show();
                    }));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to add image", Toast.LENGTH_SHORT).show();
        }
    }

    // Load images asynchronously, use cache for speed
    private void loadUserImagesInfo() {
        imageList.clear();
        progressBar.setVisibility(View.VISIBLE);
        tvNoImages.setVisibility(View.GONE);

        dbRef.child(currentUser.getUid()).child("images")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tvNoImages.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        for (DataSnapshot imageSnap : snapshot.getChildren()) {
                            String imageId = imageSnap.getKey();
                            String base64 = imageSnap.child("image").getValue(String.class);
                            if (base64 == null || base64.trim().isEmpty()) continue;

                            UserImageModel model = new UserImageModel(base64, 0, 0.0, false, null, null);
                            imageList.add(model);

                            // decode async & cache
                            if (!imageCache.containsKey(imageId)) {
                                new Thread(() -> {
                                    try {
                                        byte[] bytes = Base64.decode(base64.replaceAll("\\s+", ""), Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        imageCache.put(imageId, bitmap);
                                        runOnUiThread(adapter::notifyDataSetChanged);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Bitmap decode error", e);
                                    }
                                }).start();
                            }
                        }

                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(UploadImageActivity.this, "Error loading images", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {

        private final ArrayList<UserImageModel> images;

        public ImagesAdapter(ArrayList<UserImageModel> images) {
            this.images = images;
        }

        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            UserImageModel image = images.get(position);

            try {
                if (image.getBase64() == null || image.getBase64().trim().isEmpty()) return;

                String imageId = image.getBase64().hashCode() + "_" + position;

                // Load from cache if available
                Bitmap bitmap = imageCache.get(imageId);
                if (bitmap != null) {
                    holder.img.setImageBitmap(bitmap);
                } else {
                    // decode async with Glide
                    byte[] decoded = Base64.decode(image.getBase64().replaceAll("\\s+", ""), Base64.DEFAULT);
                    Glide.with(holder.img.getContext())
                            .asBitmap()
                            .load(decoded)
                            .placeholder(R.drawable.placeholder)
                            .into(holder.img);
                }

                if (image.isUploading()) {
                    holder.progress.setVisibility(View.VISIBLE);
                    holder.overlay.setVisibility(View.VISIBLE);
                    AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                    fadeIn.setDuration(400);
                    holder.overlay.startAnimation(fadeIn);
                } else {
                    holder.progress.setVisibility(View.GONE);
                    holder.overlay.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                Log.e(TAG, "onBindViewHolder error", e);
            }
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            ProgressBar progress;
            View overlay;

            ImageViewHolder(View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.imgUserImg);
                progress = itemView.findViewById(R.id.progressItem);
                overlay = itemView.findViewById(R.id.overlayItem);
            }
        }
    }
}
