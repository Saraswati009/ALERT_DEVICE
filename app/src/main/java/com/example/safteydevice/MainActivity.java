package com.example.safteydevice;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements contactAdapter.OnContactActionListener {
    private static final int REQUEST_CALL_PERMISSION = 1;
    private static final int REQUEST_SMS_LOCATION_PERMISSION = 2;

    private List<Contact> contactList;
    private contactAdapter contactAdapter;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize location client for location retrieval
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load saved contacts when the app starts
        contactList = loadContacts();

        // Set up RecyclerView for displaying contacts
        RecyclerView recyclerView = findViewById(R.id.contactsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactAdapter = new contactAdapter(contactList, this);
        recyclerView.setAdapter(contactAdapter);
        updateContactsUI();

        // Set up buttons
        ImageButton sosButton = findViewById(R.id.imageButton);
        ImageButton addContactsButton = findViewById(R.id.imageButton4);
        ImageButton emergencyDialButton = findViewById(R.id.imageButton5);

        // Set up onClick listeners for each button
        sosButton.setOnClickListener(view -> sendEmergencyMessage());
        addContactsButton.setOnClickListener(view -> openAddContactDialog(null, -1));
        emergencyDialButton.setOnClickListener(view -> dialSavedContact());
    }

    private void updateContactsUI() {
        TextView noContactsTextView = findViewById(R.id.noContactsTextView);
        if (contactList.isEmpty()) {
            noContactsTextView.setVisibility(View.VISIBLE);
        } else {
            noContactsTextView.setVisibility(View.GONE);
        }
        contactList = loadContacts();  // Reload contacts if necessary
        contactAdapter.notifyDataSetChanged();  // Refresh the RecyclerView
    }

    private void openAddContactDialog(Contact contactToEdit, int position) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_contact, null);

        EditText editTextContactName = dialogView.findViewById(R.id.editTextContactName);
        EditText editTextPhoneNumber = dialogView.findViewById(R.id.editTextPhoneNumber);

        if (contactToEdit != null) {
            editTextContactName.setText(contactToEdit.getName());
            editTextPhoneNumber.setText(contactToEdit.getPhone());
        }

        new AlertDialog.Builder(this)
                .setTitle(contactToEdit == null ? "Add Contact" : "Edit Contact")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = editTextContactName.getText().toString();
                    String phone = editTextPhoneNumber.getText().toString();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        if (contactToEdit == null) {
                            contactList.add(new Contact(name, phone));
                            contactAdapter.notifyItemInserted(contactList.size() - 1);
                        } else {
                            contactToEdit.setName(name);
                            contactToEdit.setPhone(phone);
                            contactAdapter.notifyItemChanged(position);
                        }
                        saveContacts(contactList);
                        updateContactsUI();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveContacts(List<Contact> contacts) {
        SharedPreferences sharedPreferences = getSharedPreferences("Contacts", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(contacts);
        editor.putString("contacts_list", json);
        editor.apply();
    }

    private List<Contact> loadContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences("Contacts", MODE_PRIVATE);
        String json = sharedPreferences.getString("contacts_list", null);

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();

        return json == null ? new ArrayList<>() : gson.fromJson(json, type);
    }

    private void sendEmergencyMessage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_SMS_LOCATION_PERMISSION);
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null && !contactList.isEmpty()) {
                        String message = "Emergency! My location is: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                        //String phoneNumber = contactList.get(9).getPhone(); // Sending to first contact
                        // Send the message to each contact
                        SmsManager smsManager = SmsManager.getDefault();
                        for (Contact contact : contactList) {
                            String contactNumber = contact.getPhone();
                            smsManager.sendTextMessage(contactNumber, null, message, null, null);
                        }

                        //sendSMSMessage(phoneNumber, message);
                    } else {
                        Toast.makeText(MainActivity.this, "Unable to get location or no contact available.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void sendSMSMessage(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Emergency message sent!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void dialSavedContact() {
        if (!contactList.isEmpty()) {
            String[] contactNames = new String[contactList.size()];
            for (int i = 0; i < contactList.size(); i++) {
                contactNames[i] = contactList.get(i).getName() + " (" + contactList.get(i).getPhone() + ")";
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select Emergency Contact")
                    .setItems(contactNames, (dialog, which) -> {
                        String phoneNumber = contactList.get(which).getPhone();
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
                        } else {
                            startCallIntent(phoneNumber);
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("No Contact Found")
                    .setMessage("Please add an emergency contact first.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private void startCallIntent(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dialSavedContact();
            } else {
                Toast.makeText(this, "Permission to make calls is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SMS_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                sendEmergencyMessage();
            } else {
                Toast.makeText(this, "Permissions are required to send emergency messages.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onEdit(Contact contact, int position) {
        openAddContactDialog(contact, position);
    }

    @Override
    public void onDelete(Contact contact, int position) {
        contactList.remove(position);
        saveContacts(contactList);
        contactAdapter.notifyItemRemoved(position);
        updateContactsUI();
    }
}










