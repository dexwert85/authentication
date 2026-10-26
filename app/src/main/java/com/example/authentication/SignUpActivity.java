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

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etSignUpEmail, etSignUpPassword;
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

        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPassword = findViewById(R.id.etSignUpPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        btnSignUp.setOnClickListener(v -> {
            String email = etSignUpEmail.getText().toString().trim();
            String password = etSignUpPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }
}