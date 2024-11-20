package com.example.safteydevice;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class contactAdapter extends RecyclerView.Adapter<contactAdapter.ContactViewHolder> {
    private final List<Contact> contactList;
    private final OnContactActionListener listener;

    // Interface to handle edit and delete actions
    public interface OnContactActionListener {
        void onEdit(Contact contact, int position);
        void onDelete(Contact contact, int position);
    }

    // Constructor to initialize contact list and action listener
    public contactAdapter(List<Contact> contactList, OnContactActionListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        // Log to check if data is being bound
        Log.d("contactAdapter", "Binding contact: " + contact.getName() + ", " + contact.getPhone());

        // Set contact name and phone number
        holder.nameTextView.setText(
                holder.itemView.getContext().getString(
                        R.string.contact_name_with_phone,
                        contact.getName(),
                        contact.getPhone()
                )
        );

        // Set click listener for Edit button
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(contact, holder.getAdapterPosition()); // Call the edit method
            }
        });

        // Set click listener for Delete button
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(contact, holder.getAdapterPosition()); // Pass both contact and position
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        Button editButton, deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contactNameTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}


