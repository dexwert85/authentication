package com.example.authentication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ClientDetailsActivity extends AppCompatActivity {

    private TextView tvDetailName, tvDetailBalance;
    private Button btnChange, btnRemoveClient, btnBackDetail;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String clientId;
    private String clientName;
    private double currentBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailBalance = findViewById(R.id.tvDetailBalance);
        btnChange = findViewById(R.id.btnChange);
        btnRemoveClient = findViewById(R.id.btnRemoveClient);
        btnBackDetail = findViewById(R.id.btnBackDetail);

        clientId = getIntent().getStringExtra("CLIENT_ID");
        clientName = getIntent().getStringExtra("CLIENT_NAME");
        currentBalance = getIntent().getDoubleExtra("CLIENT_BALANCE", 0.0);

        updateUI();

        btnChange.setOnClickListener(v -> {
            Intent intent = new Intent(ClientDetailsActivity.this, TransactionActivity.class);
            intent.putExtra("CLIENT_ID", clientId);
            intent.putExtra("CLIENT_NAME", clientName);
            intent.putExtra("CLIENT_BALANCE", currentBalance);
            startActivity(intent);
        });

        btnRemoveClient.setOnClickListener(v -> handleRemoveClient());

        btnBackDetail.setOnClickListener(v -> finish());
    }

    private void handleRemoveClient() {
        // Use a tiny threshold (0.001) to avoid Java double floating-point precision issues
        if (Math.abs(currentBalance) > 0.001) {
            Toast.makeText(this, "Cannot remove client: Balance must be $0.00", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || clientId == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Remove Client")
                .setMessage("Are you sure you want to delete " + clientName + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    btnRemoveClient.setEnabled(false);

                    db.collection("users").document(user.getUid())
                            .collection("clients").document(clientId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Client removed", Toast.LENGTH_SHORT).show();
                                finish(); // Returns to ClientListActivity, which automatically refreshes
                            })
                            .addOnFailureListener(e -> {
                                btnRemoveClient.setEnabled(true);
                                Toast.makeText(this, "Failed to remove: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && clientId != null) {
            db.collection("users").document(user.getUid())
                    .collection("clients").document(clientId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Client client = doc.toObject(Client.class);
                            if (client != null) {
                                clientName = client.getName();
                                currentBalance = client.getBalance();
                                updateUI();
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to refresh balance", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void updateUI() {
        tvDetailName.setText(clientName);
        tvDetailBalance.setText(String.format(Locale.US, "Balance: $%.2f", currentBalance));
        tvDetailBalance.setTextColor(currentBalance < 0 ? Color.RED : Color.parseColor("#2E7D32"));
    }
}