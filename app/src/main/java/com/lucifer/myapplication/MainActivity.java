package com.lucifer.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View; // Import View
import android.content.Intent; // Import Intent
import android.widget.Button; // Import Button
import android.widget.EditText; // Import EditText
import androidx.core.splashscreen.SplashScreen; // Import SplashScreen
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText editTextId;
    private EditText editTextPassword;
    private Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //install splash first
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        //then proceed with normal flow
        super.onCreate(savedInstanceState);
        // optional: keep splash a bit longer while you prep data
        boolean[] isDataReady = new boolean[1];
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> isDataReady[0] = true,
                1000 // Keep splash for 1 seconds for demonstration
        );
        splashScreen.setKeepOnScreenCondition(() -> !isDataReady[0]);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editTextId = findViewById(R.id.edit);
        editTextPassword = findViewById(R.id.password);
        buttonSubmit = findViewById(R.id.button);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editTextId.getText().toString();
                String password = editTextPassword.getText().toString();
                Intent intent = new Intent(MainActivity.this, DisplayActivity.class);
                startActivity(intent);
            }
        });


        Button register = findViewById(R.id.button2);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start RegisterActivity
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
}