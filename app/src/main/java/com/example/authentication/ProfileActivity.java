package com.example.authentication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileAddress;
    private EditText etProfileName, etProfileEmail, etProfilePhone, etProfileAddress;
    private Button btnEdit, btnBack, btnSave, btnCancel;
    private LinearLayout layoutEditButtons;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isEditMode = false;
    private Uri selectedImageUri = null;
    private String currentBase64Image = null;

    // Gallery Launcher (Shifted to background thread to prevent UI freezing)
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imgProfile.setImageURI(uri);
                    selectedImageUri = uri;
                    Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

                    new Thread(() -> {
                        String base64 = convertUriToBase64(uri);
                        runOnUiThread(() -> currentBase64Image = base64);
                    }).start();
                }
            });

    // Camera Launcher
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);

                    new Thread(() -> {
                        selectedImageUri = saveBitmapToCache(bitmap);
                        String base64 = convertBitmapToBase64(bitmap);
                        runOnUiThread(() -> currentBase64Image = base64);
                    }).start();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imgProfile = findViewById(R.id.imgProfile);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileAddress = findViewById(R.id.tvProfileAddress);

        etProfileName = findViewById(R.id.etProfileName);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileAddress = findViewById(R.id.etProfileAddress);

        btnEdit = findViewById(R.id.btnEdit);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        layoutEditButtons = findViewById(R.id.layoutEditButtons);

        loadUserData();

        imgProfile.setOnClickListener(v -> {
            if (isEditMode) {
                showImageSelectionDialog();
            } else {
                Toast.makeText(this, "Click 'Edit Profile' to change photo", Toast.LENGTH_SHORT).show();
            }
        });

        btnEdit.setOnClickListener(v -> setEditMode(true));
        btnCancel.setOnClickListener(v -> setEditMode(false));
        btnSave.setOnClickListener(v -> saveProfileChanges());
        btnBack.setOnClickListener(v -> finish());
    }

    private void showImageSelectionDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                cameraLauncher.launch(null);
            } else if (which == 1) {
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }

    private void setEditMode(boolean enable) {
        isEditMode = enable;

        if (enable) {
            etProfileName.setText(tvProfileName.getText().toString());
            etProfileEmail.setText(tvProfileEmail.getText().toString());
            etProfilePhone.setText(tvProfilePhone.getText().toString().equals("Not provided") ? "" : tvProfilePhone.getText().toString());
            etProfileAddress.setText(tvProfileAddress.getText().toString().equals("Not provided") ? "" : tvProfileAddress.getText().toString());

            etProfileName.setVisibility(View.VISIBLE);
            etProfileEmail.setVisibility(View.VISIBLE);
            etProfilePhone.setVisibility(View.VISIBLE);
            etProfileAddress.setVisibility(View.VISIBLE);
            layoutEditButtons.setVisibility(View.VISIBLE);

            tvProfileName.setVisibility(View.GONE);
            tvProfileEmail.setVisibility(View.GONE);
            tvProfilePhone.setVisibility(View.GONE);
            tvProfileAddress.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
            btnBack.setVisibility(View.GONE);
        } else {
            etProfileName.setVisibility(View.GONE);
            etProfileEmail.setVisibility(View.GONE);
            etProfilePhone.setVisibility(View.GONE);
            etProfileAddress.setVisibility(View.GONE);
            layoutEditButtons.setVisibility(View.GONE);

            tvProfileName.setVisibility(View.VISIBLE);
            tvProfileEmail.setVisibility(View.VISIBLE);
            tvProfilePhone.setVisibility(View.VISIBLE);
            tvProfileAddress.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.VISIBLE);
        }
    }

    private void saveProfileChanges() {
        String newName = etProfileName.getText().toString().trim();
        String newEmail = etProfileEmail.getText().toString().trim();
        String newPhone = etProfilePhone.getText().toString().trim();
        String newAddress = etProfileAddress.getText().toString().trim();

        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // 1. Send data to Firestore (Fire-and-forget)
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", newName);
        userData.put("email", newEmail);
        userData.put("phone", newPhone);
        userData.put("address", newAddress);
        if (currentBase64Image != null) {
            userData.put("profileImageBase64", currentBase64Image);
        }
        db.collection("users").document(user.getUid()).set(userData, SetOptions.merge());

        // 2. Update Firebase Auth Profile (Fire-and-forget)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        user.updateProfile(profileUpdates);

        // 3. Update Auth Email ONLY if it changed
        if (!newEmail.equals(user.getEmail())) {
            user.updateEmail(newEmail).addOnFailureListener(e -> {
                // Firebase requires a "recent login" to change emails.
                // If it fails, we show a toast, but we don't freeze the app.
                Toast.makeText(ProfileActivity.this, "Note: To change login email, please sign out and sign back in first.", Toast.LENGTH_LONG).show();
            });
        }

        // 4. Update the UI immediately so the app feels fast and snappy
        tvProfileName.setText(newName.isEmpty() ? "User Name" : newName);
        tvProfileEmail.setText(newEmail);
        tvProfilePhone.setText(newPhone.isEmpty() ? "Not provided" : newPhone);
        tvProfileAddress.setText(newAddress.isEmpty() ? "Not provided" : newAddress);

        Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
        resetSaveButton();
        setEditMode(false);
    }

    private void resetSaveButton() {
        btnSave.setEnabled(true);
        btnSave.setText("Save");
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.reload().addOnCompleteListener(reloadTask -> {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phone");
                            String address = documentSnapshot.getString("address");
                            String base64Image = documentSnapshot.getString("profileImageBase64");

                            String authName = user.getDisplayName();
                            String authEmail = user.getEmail();

                            tvProfileName.setText(name != null && !name.isEmpty() ? name : (authName != null && !authName.isEmpty() ? authName : "User Name"));
                            tvProfileEmail.setText(email != null && !email.isEmpty() ? email : (authEmail != null ? authEmail : ""));
                            tvProfilePhone.setText(phone != null && !phone.isEmpty() ? phone : "Not provided");
                            tvProfileAddress.setText(address != null && !address.isEmpty() ? address : "Not provided");

                            // Load base64 image on a background thread so it doesn't freeze the app on launch
                            if (base64Image != null && !base64Image.isEmpty()) {
                                currentBase64Image = base64Image;
                                new Thread(() -> {
                                    Bitmap bitmap = decodeBase64ToBitmap(base64Image);
                                    if (bitmap != null) {
                                        runOnUiThread(() -> imgProfile.setImageBitmap(bitmap));
                                    }
                                }).start();
                            }
                        } else {
                            tvProfileName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "User Name");
                            tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                            tvProfilePhone.setText("Not provided");
                            tvProfileAddress.setText("Not provided");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // Safely reads image boundaries first to prevent Out Of Memory crashes/freezes
    private String convertUriToBase64(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            if (input != null) input.close();

            // Calculate shrink ratio
            int maxDim = 500;
            int inSampleSize = 1;
            if (options.outHeight > maxDim || options.outWidth > maxDim) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                    inSampleSize *= 2;
                }
            }

            // Decode safely
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (input != null) input.close();

            return convertBitmapToBase64(bitmap);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        int maxDimension = 500;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);

        Bitmap scaledBitmap = bitmap;
        if (ratio < 1.0f) {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (width * ratio), (int) (height * ratio), true);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri saveBitmapToCache(Bitmap bitmap) {
        File cachePath = new File(getCacheDir(), "images");
        cachePath.mkdirs();
        File file = new File(cachePath, "profile_temp.png");
        try (FileOutputStream stream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file);
    }
}