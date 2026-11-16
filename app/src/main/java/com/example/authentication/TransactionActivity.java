package com.example.authentication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class TransactionActivity extends AppCompatActivity {

    private EditText etAmount;
    private Button btnDeposit, btnWithdraw;
    private FirebaseFirestore db;

    private String clientId;
    private String clientName;
    private double currentBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        etAmount = findViewById(R.id.etAmount);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnWithdraw = findViewById(R.id.btnWithdraw);

        clientId = getIntent().getStringExtra("CLIENT_ID");
        clientName = getIntent().getStringExtra("CLIENT_NAME");
        currentBalance = getIntent().getDoubleExtra("CLIENT_BALANCE", 0.0);

        btnDeposit.setOnClickListener(v -> handleTransaction(true));
        btnWithdraw.setOnClickListener(v -> handleTransaction(false));
    }

    private void handleTransaction(boolean isDeposit) {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        double delta = isDeposit ? amount : -amount;
        double newBalance = currentBalance + delta; // Allowed to go negative
        String typeLabel = isDeposit ? "Deposit" : "Withdrawal";

        new AlertDialog.Builder(this)
                .setTitle("Confirm " + typeLabel)
                .setMessage(String.format(Locale.US,
                        "Client: %s\n%s: $%.2f\nNew Balance: $%.2f",
                        clientName, typeLabel, amount, newBalance))
                .setPositiveButton("Confirm", (dialog, which) -> {
                    btnDeposit.setEnabled(false);
                    btnWithdraw.setEnabled(false);

                    db.collection("clients").document(clientId)
                            .update("balance", FieldValue.increment(delta))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, typeLabel + " saved!", Toast.LENGTH_SHORT).show();
                                finish(); // Returns to ClientDetailActivity, which refreshes automatically
                            })
                            .addOnFailureListener(e -> {
                                btnDeposit.setEnabled(true);
                                btnWithdraw.setEnabled(true);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}