package com.lucifer.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class RegisterActivity extends BaseActivity {

    private EditText editTextId;
    private EditText editTextPassword;
    private Button buttonSubmit;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registor);

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        editTextId = findViewById(R.id.editTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSubmit = findViewById(R.id.button3);

        // Check if userId was prefilled from MainActivity
        Intent receivedIntent = getIntent();
        String prefilledUserId = receivedIntent.getStringExtra("prefilledUserId");
        if (prefilledUserId != null && !prefilledUserId.isEmpty()) {
            editTextId.setText(prefilledUserId);
        }

        // REGISTER BUTTON - Save user data and redirect
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                // Validate input fields
                if (userId.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill in both User ID and Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate password length
                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if user already exists before registering
                checkAndRegisterUser(userId, password);
            }
        });

        // BACK TO LOGIN BUTTON
        Button buttonToMain = findViewById(R.id.button4);
        buttonToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to MainActivity
            }
        });
    }

    // Method to check if user exists and register if not
    private void checkAndRegisterUser(String userId, String password) {
        Toast.makeText(this, "Checking user availability...", Toast.LENGTH_SHORT).show();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User already exists
                    Toast.makeText(RegisterActivity.this, "User ID already exists. Please choose a different one.", Toast.LENGTH_LONG).show();
                } else {
                    // User doesn't exist, proceed with registration
                    registerNewUser(userId, password);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(RegisterActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method to register new user in Firebase
    private void registerNewUser(String userId, String password) {
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();

        // Create user data map
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("password", password);
        userMap.put("userId", userId);
        userMap.put("registrationTime", System.currentTimeMillis());

        // Save user data to Firebase
        mDatabase.child("users").child(userId).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    // Registration successful
                    Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                    // Redirect to DisplayActivity
                    Intent intent = new Intent(RegisterActivity.this, DisplayActivity.class);
                    intent.putExtra("userId", userId); // Pass userId to next activity
                    intent.putExtra("isNewUser", true); // Optional: indicate this is a new user
                    startActivity(intent);

                    // Close registration activity
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Registration failed
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
