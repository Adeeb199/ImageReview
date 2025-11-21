package com.mariaxcodexpert.imagereview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        btnAddImage = findViewById(R.id.btnAddImage);
        rvImages = findViewById(R.id.rvImages);
        tvNoImages = findViewById(R.id.tvNoImages);
        progressBar = findViewById(R.id.progressBar);

        rvImages.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new UploadImageAdapter(imageList);
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

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            uploadImage(data.getData());
        }
    }

    private void uploadImage(Uri imageUri) {
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

            UserImageModel tempModel = new UserImageModel(encodedImage, 0, 0.0, true, imageId, currentUser.getUid());

            imageList.add(0, tempModel);
            adapter.notifyItemInserted(0);

            DatabaseReference ref = dbRef.child(currentUser.getUid())
                    .child("images").child(imageId);

            ref.child("image").setValue(encodedImage);
            ref.child("timestamp").setValue(System.currentTimeMillis())
                    .addOnCompleteListener(task -> {
                        tempModel.setUploading(false);
                        adapter.notifyItemChanged(0);
                        Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
        }
    }

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

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String base64 = snap.child("image").getValue(String.class);
                            if (base64 != null) {
                                String imageId = snap.getKey();
                                String ownerUid = currentUser.getUid();
                                imageList.add(new UserImageModel(base64, 0, 0.0, false, imageId, ownerUid));
                            }
                        }


                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }
}
