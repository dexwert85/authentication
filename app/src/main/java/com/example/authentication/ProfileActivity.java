package com.example.authentication;

import android.graphics.Bitmap;
import android.os.Bundle;
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

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileAddress;
    private EditText etProfileName, etProfileEmail, etProfilePhone, etProfileAddress;
    private Button btnEdit, btnBack, btnSave, btnCancel;
    private LinearLayout layoutEditButtons;

    private FirebaseAuth mAuth;
    private boolean isEditMode = false;

    // Gallery Picker Launcher
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imgProfile.setImageURI(uri);
                }
            });

    // Camera Launcher
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
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

        // Image Click Listener
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

        tvProfileName.setText(newName.isEmpty() ? "User Name" : newName);
        tvProfileEmail.setText(newEmail);
        tvProfilePhone.setText(newPhone.isEmpty() ? "Not provided" : newPhone);
        tvProfileAddress.setText(newAddress.isEmpty() ? "Not provided" : newAddress);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (!newName.isEmpty()) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();
                user.updateProfile(profileUpdates);
            }

            if (!newEmail.equals(user.getEmail())) {
                user.updateEmail(newEmail)
                        .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to update email: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
        setEditMode(false);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();

            tvProfileName.setText(name != null && !name.isEmpty() ? name : "User Name");
            tvProfileEmail.setText(email != null ? email : "");
            tvProfilePhone.setText("Not provided");
            tvProfileAddress.setText("Not provided");
        }
    }
}