package com.example.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientListActivity extends AppCompatActivity {

    private ListView lvClients;
    private Button btnAddClient;
    private ClientListAdapter adapter;
    private List<Client> clientList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        lvClients = findViewById(R.id.lvClients);
        btnAddClient = findViewById(R.id.btnAddClient);

        clientList = new ArrayList<>();
        adapter = new ClientListAdapter(this, clientList);
        lvClients.setAdapter(adapter);

        // Open client details when a row is pressed
        lvClients.setOnItemClickListener((parent, view, position, id) -> {
            Client selectedClient = clientList.get(position);
            Intent intent = new Intent(ClientListActivity.this, ClientDetailsActivity.class);
            intent.putExtra("CLIENT_ID", selectedClient.getId());
            intent.putExtra("CLIENT_NAME", selectedClient.getName());
            intent.putExtra("CLIENT_BALANCE", selectedClient.getBalance());
            startActivity(intent);
        });

        btnAddClient.setOnClickListener(v -> showAddClientDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClients(); // Refresh list every time we return to this screen
    }

    private void loadClients() {
        db.collection("clients")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    clientList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Client client = doc.toObject(Client.class);
                        clientList.add(client);
                    }

                    // Sort numerically by ID (0, 1, 2, ... 10)
                    Collections.sort(clientList, (c1, c2) -> {
                        try {
                            return Integer.compare(Integer.parseInt(c1.getId()), Integer.parseInt(c2.getId()));
                        } catch (NumberFormatException e) {
                            return c1.getId().compareTo(c2.getId());
                        }
                    });

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading clients: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showAddClientDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 12);

        EditText etName = new EditText(this);
        etName.setHint("Client Name");
        layout.addView(etName);

        EditText etInitialBalance = new EditText(this);
        etInitialBalance.setHint("Starting Balance (default 0)");
        etInitialBalance.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(etInitialBalance);

        new AlertDialog.Builder(this)
                .setTitle("New Client")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double balance = 0.0;
                    String balStr = etInitialBalance.getText().toString().trim();
                    if (!balStr.isEmpty()) {
                        try {
                            balance = Double.parseDouble(balStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid balance number", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    createClientWithAutoId(name, balance);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createClientWithAutoId(String name, double balance) {
        // Determine next sequential ID: 0, 1, 2, ...
        int nextIdInt = 0;
        for (Client c : clientList) {
            try {
                int currentId = Integer.parseInt(c.getId());
                if (currentId >= nextIdInt) {
                    nextIdInt = currentId + 1;
                }
            } catch (NumberFormatException ignored) {}
        }

        String autoId = String.valueOf(nextIdInt);
        Client newClient = new Client(autoId, name, balance);

        db.collection("clients").document(autoId)
                .set(newClient)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Client #" + autoId + " added", Toast.LENGTH_SHORT).show();
                    loadClients();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}