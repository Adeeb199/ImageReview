package com.mariaxcodexpert.imagereview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class UploadImageActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;

    private ImageView btnAddImage;
    private RecyclerView rvImages;
    private TextView tvNoImages;
    private ProgressBar progressBar;

    private UploadImageAdapter adapter;
    private ArrayList<UserImageModel> imageList = new ArrayList<>();

    private FirebaseUser currentUser;
    private DatabaseReference dbRef;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        bindUI();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance(
                "https://imagereview397-default-rtdb.firebaseio.com/"
        ).getReference("users");

        storageRef = FirebaseStorage.getInstance().getReference("user_images");

        setupRecyclerView();

        btnAddImage.setOnClickListener(v -> openFileChooser());

        loadUserImages();
    }

    private void bindUI() {
        btnAddImage = findViewById(R.id.btnAddImage);
        rvImages = findViewById(R.id.rvImages);
        tvNoImages = findViewById(R.id.tvNoImages);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new UploadImageAdapter(imageList);
        rvImages.setAdapter(adapter);
    }

    // ---------- Select Image ----------
    private void openFileChooser() {
        Intent pick = new Intent();
        pick.setType("image/*");
        pick.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(pick, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) uploadImageToStorage(uri);
        }
    }

    // ---------- Upload Image to Firebase Storage ----------
    private void uploadImageToStorage(Uri imageUri) {

        byte[] compressedImage = compressImage(imageUri);
        if (compressedImage == null) {
            Toast.makeText(this, "Image compress nahi hui", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageId = String.valueOf(System.currentTimeMillis());
        StorageReference imgRef = storageRef.child(currentUser.getUid()).child(imageId + ".jpg");

        // Immediate UI feedback
        UserImageModel uploadingModel = new UserImageModel("", 0, 0.0, true, imageId, currentUser.getUid());
        imageList.add(0, uploadingModel);
        adapter.notifyItemInserted(0);

        UploadTask uploadTask = imgRef.putBytes(compressedImage);

        uploadTask.addOnSuccessListener(taskSnapshot ->
                imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    saveImageInfoToDatabase(downloadUrl, imageId, uploadingModel);
                })
        ).addOnFailureListener(e -> {
            imageList.remove(uploadingModel);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // ---------- Compress Image ----------
    private byte[] compressImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------- Save Info to Realtime DB ----------
    private void saveImageInfoToDatabase(String url, String imageId, UserImageModel model) {
        model.setImageUrl(url);
        model.setUploading(false);

        dbRef.child(currentUser.getUid())
                .child("images")
                .child(imageId)
                .setValue(model.toFirebaseMap())
                .addOnCompleteListener(task -> Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "DB update failed", Toast.LENGTH_SHORT).show());

        adapter.notifyItemChanged(imageList.indexOf(model));
    }

    // ---------- Load All User Images ----------
    private void loadUserImages() {

        imageList.clear();
        progressBar.setVisibility(View.VISIBLE);
        tvNoImages.setVisibility(View.GONE);

        dbRef.child(currentUser.getUid()).child("images")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (!snapshot.exists()) {
                            tvNoImages.setVisibility(View.VISIBLE);
                            return;
                        }
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String url = snap.child("imageUrl").getValue(String.class);
                            if (url != null) {
                                int reviews = snap.child("reviewCount").getValue(Integer.class) != null ?
                                        snap.child("reviewCount").getValue(Integer.class) : 0;
                                double avg = snap.child("avgRating").getValue(Double.class) != null ?
                                        snap.child("avgRating").getValue(Double.class) : 0.0;
                                boolean uploading = snap.child("uploading").getValue(Boolean.class) != null ?
                                        snap.child("uploading").getValue(Boolean.class) : false;
                                String imageId = snap.getKey();
                                imageList.add(new UserImageModel(url, reviews, avg, uploading, imageId, currentUser.getUid()));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UploadImageActivity.this, "Failed to load", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
