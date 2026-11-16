package com.example.authentication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ClientDetailsActivity extends AppCompatActivity {

    private TextView tvDetailName, tvDetailBalance;
    private Button btnChange, btnBackDetail;
    private FirebaseFirestore db;

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

        db = FirebaseFirestore.getInstance();
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailBalance = findViewById(R.id.tvDetailBalance);
        btnChange = findViewById(R.id.btnChange);
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

        btnBackDetail.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clientId != null) {
            db.collection("clients").document(clientId)
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