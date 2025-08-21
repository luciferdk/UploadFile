package com.lucifer.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DisplayActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_display); // Make sure this matches your XML file name
            // Add any other initialization for RegisterActivity here
            Button buttonToMain = findViewById(R.id.button6);
            buttonToMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Hello");
                }
            });
        }
    }


