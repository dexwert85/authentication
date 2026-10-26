package com.example.authentication;

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

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etResetEmail;
    private Button btnResetPassword;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forget_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        etResetEmail = findViewById(R.id.etResetEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnResetPassword.setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button to prevent double-clicking
            btnResetPassword.setEnabled(false);

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        // Re-enable the button
                        btnResetPassword.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent! Check your inbox.", Toast.LENGTH_LONG).show();
                        } else {
                            // Show the exact exception message from Firebase
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(ForgotPasswordActivity.this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }
}