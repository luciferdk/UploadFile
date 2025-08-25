package com.lucifer.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; // Add Toast import for user feedback
import androidx.core.splashscreen.SplashScreen;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener; // Add this import

import java.util.HashMap;

public class MainActivity extends BaseActivity {

    private EditText editTextId;
    private EditText editTextPassword;
    private Button buttonSubmit;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        boolean[] isDataReady = new boolean[1];
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> isDataReady[0] = true, 1000
        );
        splashScreen.setKeepOnScreenCondition(() -> !isDataReady[0]);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editTextId = findViewById(R.id.edit);
        editTextPassword = findViewById(R.id.password);
        buttonSubmit = findViewById(R.id.button);

        // LOGIN BUTTON - Check credentials before redirecting
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (userId.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter both User ID and Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check credentials against Firebase
                checkUserCredentials(userId, password);
            }
        });

        // REGISTER BUTTON
        Button register = findViewById(R.id.button2);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Method to check user credentials against Firebase
    private void checkUserCredentials(String userId, String password) {
        Toast.makeText(this, "Checking credentials...", Toast.LENGTH_SHORT).show();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User exists, check password
                    String savedPassword = dataSnapshot.child("password").getValue(String.class);

                    if (savedPassword != null && savedPassword.equals(password)) {
                        // Credentials match - redirect to DisplayActivity
                        Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, DisplayActivity.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                        finish();
                    } else {
                        // Password doesn't match - redirect to register
                        Toast.makeText(MainActivity.this, "Invalid password. Redirecting to registration...", Toast.LENGTH_LONG).show();
                        redirectToRegister();
                    }
                } else {
                    // User doesn't exist - redirect to register
                    Toast.makeText(MainActivity.this, "User not found. Redirecting to registration...", Toast.LENGTH_LONG).show();
                    redirectToRegister();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method to redirect to registration page
    private void redirectToRegister() {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        String userId = editTextId.getText().toString().trim();
        if (!userId.isEmpty()) {
            intent.putExtra("prefilledUserId", userId);
        }
        startActivity(intent);
    }
}
