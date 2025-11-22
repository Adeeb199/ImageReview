package com.mariaxcodexpert.imagereview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int REQ_ONE_TAP = 2;
    private static final String TAG = "LoginActivity";

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseAuth firebaseAuth;

    private LinearLayout btnGoogleSignIn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.loginProgressBar);

        firebaseAuth = FirebaseAuth.getInstance();

        // Ultra-fast pre-check
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            launchMain(currentUser);
            return;
        }

        oneTapClient = Identity.getSignInClient(this);

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                )
                .setAutoSelectEnabled(true) // super fast returning users
                .build();

        btnGoogleSignIn.setOnClickListener(v -> startSignIn());
    }

    private void startSignIn() {
        progressBar.setVisibility(View.VISIBLE);

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP, null, 0, 0, 0
                        );
                    } catch (Exception e) {
                        handleSignInError(e);
                    }
                })
                .addOnFailureListener(this, this::handleSignInError);
    }

    private void handleSignInError(Exception e) {
        progressBar.setVisibility(View.GONE);
        Log.e(TAG, "Sign-In Error", e);
        Toast.makeText(this, "Sign-In Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQ_ONE_TAP) return;
        progressBar.setVisibility(View.VISIBLE);

        try {
            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();

            if (idToken != null) {
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

                // Single-line advanced sign-in with completion listener
                firebaseAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                if (user != null) initUserInDatabase(user);
                                launchMain(user);
                            } else {
                                Log.e(TAG, "Firebase Auth Failed", task.getException());
                                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "No ID token!", Toast.LENGTH_SHORT).show();
            }

        } catch (ApiException e) {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Google Sign-In Error", e);
            Toast.makeText(this, "Sign-In Failed", Toast.LENGTH_SHORT).show();
        }
    }

    // Advanced DB write: batch in one call
    private void initUserInDatabase(@NonNull FirebaseUser user) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.getDisplayName());
        userMap.put("email", user.getEmail());
        userMap.put("lastLogin", System.currentTimeMillis());

        dbRef.child(user.getUid()).updateChildren(userMap)
                .addOnFailureListener(e -> Log.e(TAG, "DB Init Failed", e));
    }

    private void launchMain(FirebaseUser user) {
        Toast.makeText(this, "Welcome " + (user != null ? user.getDisplayName() : ""), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
