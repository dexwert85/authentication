package com.example.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etSignUpName, etSignUpEmail, etSignUpPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etSignUpName = findViewById(R.id.etSignUpName);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnSignUp.setOnClickListener(v -> {
            String name = etSignUpName.getText().toString().trim();
            String email = etSignUpEmail.getText().toString().trim();
            String password = etSignUpPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSignUp.setEnabled(false);
            btnSignUp.setText("Signing Up...");

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // 1. Fire-and-forget Profile Update
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates);

                                // 2. Fire-and-forget Firestore Update
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name);
                                userData.put("email", email);
                                userData.put("phone", "");
                                userData.put("address", "");
                                userData.put("profileImageBase64", "");

                                db.collection("users").document(user.getUid()).set(userData);

                                // 3. Allow the user to enter the app immediately!
                                resetSignUpButton();
                                navigateToMain();
                            }
                        } else {
                            // If Auth fails, we MUST reset the button so they can try again
                            resetSignUpButton();
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(SignUpActivity.this, "Sign up failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void resetSignUpButton() {
        btnSignUp.setEnabled(true);
        btnSignUp.setText("Sign Up");
    }

    private void navigateToMain() {
        Toast.makeText(SignUpActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        finish(); // Destroy SignUpActivity so they can't hit back to it
    }
}