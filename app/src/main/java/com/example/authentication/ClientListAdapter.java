package com.example.authentication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

public class ClientListAdapter extends ArrayAdapter<Client> {

    public ClientListAdapter(@NonNull Context context, @NonNull List<Client> clients) {
        super(context, 0, clients);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_client, parent, false);
        }

        Client client = getItem(position);
        TextView tvId = convertView.findViewById(R.id.tvItemId);
        TextView tvName = convertView.findViewById(R.id.tvItemName);
        TextView tvBalance = convertView.findViewById(R.id.tvItemBalance);

        if (client != null) {
            tvId.setText("ID: " + client.getId());
            tvName.setText(client.getName());
            tvBalance.setText(String.format(Locale.US, "$%.2f", client.getBalance()));
            tvBalance.setTextColor(client.getBalance() < 0 ? Color.RED : Color.parseColor("#2E7D32"));
        }

        return convertView;
    }
}