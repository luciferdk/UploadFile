package com.lucifer.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class DisplayActivity extends BaseActivity {

    private static final int FILE_SELECT_CODE = 0;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String currentUserId;
    private LinearLayout filesContainer;
    private Button uploadButton;
    private TextView userWelcome;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        // Get current user ID from intent
        Intent intent = getIntent();
        currentUserId = intent.getStringExtra("userId");
        boolean isNewUser = intent.getBooleanExtra("isNewUser", false);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: No user ID found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Set welcome message
        userWelcome.setText("Welcome, " + currentUserId + "!");

        // Show new user message
        if (isNewUser) {
            Toast.makeText(this, "Account created successfully! Welcome!", Toast.LENGTH_LONG).show();
        }

        // Load user's files
        loadUserFiles();

        // Set up upload button
        uploadButton.setOnClickListener(v -> openFileChooser());

        // Logout button
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());
    }

    private void initializeViews() {
        filesContainer = findViewById(R.id.filesContainer);
        uploadButton = findViewById(R.id.uploadButton);
        userWelcome = findViewById(R.id.userWelcome);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading file...");
        progressDialog.setCancelable(false);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                uploadFileToFirebase(fileUri);
            }
        }
    }

    private void uploadFileToFirebase(Uri fileUri) {
        String fileName = getFileName(fileUri);
        if (fileName == null) {
            fileName = "unnamed_file_" + System.currentTimeMillis();
        }

        progressDialog.show();

        // Create unique file path for this user
        String filePath = "files/" + currentUserId + "/" + System.currentTimeMillis() + "_" + fileName;
        StorageReference fileRef = mStorage.child(filePath);

        // Upload file
        UploadTask uploadTask = fileRef.putFile(fileUri);
        String finalFileName = fileName;
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressDialog.setMessage("Uploading: " + (int)progress + "%");
        }).addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                // Save file info to database
                saveFileInfo(finalFileName, downloadUri.toString(), filePath);
                progressDialog.dismiss();
                Toast.makeText(this, "File uploaded successfully!", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void saveFileInfo(String fileName, String downloadUrl, String filePath) {
        String fileId = mDatabase.child("user_files").child(currentUserId).push().getKey();

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("fileName", fileName);
        fileInfo.put("downloadUrl", downloadUrl);
        fileInfo.put("filePath", filePath);
        fileInfo.put("uploadTime", System.currentTimeMillis());
        fileInfo.put("fileId", fileId);

        mDatabase.child("user_files").child(currentUserId).child(fileId).setValue(fileInfo)
                .addOnSuccessListener(aVoid -> loadUserFiles())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save file info", Toast.LENGTH_SHORT).show());
    }

    private void loadUserFiles() {
        filesContainer.removeAllViews();

        mDatabase.child("user_files").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                filesContainer.removeAllViews();

                if (!dataSnapshot.exists()) {
                    TextView noFilesText = new TextView(DisplayActivity.this);
                    noFilesText.setText("No files uploaded yet.");
                    noFilesText.setTextSize(16);
                    noFilesText.setPadding(20, 20, 20, 20);
                    filesContainer.addView(noFilesText);
                    return;
                }

                for (DataSnapshot fileSnapshot : dataSnapshot.getChildren()) {
                    String fileName = fileSnapshot.child("fileName").getValue(String.class);
                    String downloadUrl = fileSnapshot.child("downloadUrl").getValue(String.class);
                    String filePath = fileSnapshot.child("filePath").getValue(String.class);
                    String fileId = fileSnapshot.child("fileId").getValue(String.class);
                    Long uploadTime = fileSnapshot.child("uploadTime").getValue(Long.class);

                    if (fileName != null && downloadUrl != null) {
                        addFileToUI(fileName, downloadUrl, filePath, fileId, uploadTime);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DisplayActivity.this, "Failed to load files", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addFileToUI(String fileName, String downloadUrl, String filePath, String fileId, Long uploadTime) {
        // Create container for each file
        LinearLayout fileContainer = new LinearLayout(this);
        fileContainer.setOrientation(LinearLayout.VERTICAL);
        fileContainer.setPadding(20, 10, 20, 10);
        fileContainer.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 10, 0, 10);
        fileContainer.setLayoutParams(containerParams);

        // File name
        TextView fileNameText = new TextView(this);
        fileNameText.setText("📄 " + fileName);
        fileNameText.setTextSize(16);
        fileNameText.setTextColor(getResources().getColor(android.R.color.black));

        // Upload time
        TextView timeText = new TextView(this);
        if (uploadTime != null) {
            timeText.setText("Uploaded: " + new java.util.Date(uploadTime).toString());
        } else {
            timeText.setText("Upload time unknown");
        }
        timeText.setTextSize(12);
        timeText.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Buttons container
        LinearLayout buttonsContainer = new LinearLayout(this);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setPadding(0, 10, 0, 0);

        // View button
        Button viewButton = new Button(this);
        viewButton.setText("View");
        viewButton.setOnClickListener(v -> viewFile(downloadUrl));

        // Download button
        Button downloadButton = new Button(this);
        downloadButton.setText("Download");
        downloadButton.setOnClickListener(v -> downloadFile(downloadUrl, fileName));

        // Delete button
        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(v -> deleteFile(fileId, filePath, fileName));

        // Add buttons to container
        buttonsContainer.addView(viewButton);
        buttonsContainer.addView(downloadButton);
        buttonsContainer.addView(deleteButton);

        // Add all elements to file container
        fileContainer.addView(fileNameText);
        fileContainer.addView(timeText);
        fileContainer.addView(buttonsContainer);

        // Add file container to main container
        filesContainer.addView(fileContainer);
    }

    private void viewFile(String downloadUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(downloadUrl));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open this file type", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFile(String downloadUrl, String fileName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(downloadUrl));

        try {
            startActivity(Intent.createChooser(intent, "Download " + fileName));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot download file", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFile(String fileId, String filePath, String fileName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete '" + fileName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Storage first
                    mStorage.child(filePath).delete().addOnSuccessListener(aVoid -> {
                        // Delete from Database
                        mDatabase.child("user_files").child(currentUserId).child(fileId).removeValue()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(DisplayActivity.this, "File deleted successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(DisplayActivity.this, "Failed to delete file info", Toast.LENGTH_SHORT).show();
                                });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(DisplayActivity.this, "Failed to delete file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close current activity and return to login
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
