package com.example.myapplication.fragments;
import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.R;
import com.example.myapplication.database.JournalEntry;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddJournalEntryFragment extends JournalEntryFragment {

    private EditText editTextTitle;
    private EditText editTextText;
    private EditText editTextDate;
    private EditText editTextLocation;
    private Button btnDatePicker;
    private Button btnUploadImage;
    private Button btnClose;
    private Button btnCaptureImage;

    private Uri imageUri;
    private ImageView closeImage;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ImageView imageView; // Reference to the ImageView where the selected image will be displayed

    private DatePickerDialog datePickerDialog;

    public AddJournalEntryFragment(LinearLayout mainFrame) {
        super(mainFrame);
    }

    private File createImageFile() throws IOException {
        // Create a unique filename for the image
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timestamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        Log.d("filename", imageFileName);

        // Create the image file
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);

        return imageFile;
    }

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    // Display the captured image in the ImageView
                    closeImage.setVisibility(View.VISIBLE);

                    imageView.setImageURI(imageUri);
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, launch the camera intent
                    launchCameraIntent();
                } else {
                    // Permission denied, show a message or handle accordingly
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });


    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the image picker launcher
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            // Use the selected image URI as needed (e.g., display the image in an ImageView)
                            closeImage.setVisibility(View.VISIBLE);
                            imageUri = result;
                            imageView.setImageURI(result);
                        }
                    }
                });

        // Initialize the views
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextText = view.findViewById(R.id.editTextText);
        editTextDate = view.findViewById(R.id.editTextDate);
        editTextLocation = view.findViewById(R.id.editTextLocation);
        btnDatePicker = view.findViewById(R.id.btnDatePicker);
        imageView = view.findViewById(R.id.imageView);

        btnUploadImage = view.findViewById(R.id.btnUploadImage);
        btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
        closeImage = view.findViewById(R.id.imgCloseImage);

        Button btnSaveEntry = view.findViewById(R.id.btnSaveEntry);

        btnSaveEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveJournalEntry();
            }
        });

        btnCaptureImage.setOnClickListener(v -> {
            // Check for necessary permission before launching the camera intent
            if (hasCameraPermission()) {
                launchCameraIntent();
            } else {
                requestCameraPermission();
            }
        });


        closeImage.setOnClickListener(v -> {
            // Clear the image from the ImageView
            imageView.setImageDrawable(null);
            // Hide the "X" button
            closeImage.setVisibility(View.GONE);
        });

        // Set click listeners
        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

    }

    private void saveJournalEntry() {
        String title = editTextTitle.getText().toString().trim();
        String text = editTextText.getText().toString().trim();
        String date = editTextDate.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();

        if (title.isEmpty() || text.isEmpty() || date.isEmpty() || location.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath ="images/" + imageUri.getLastPathSegment();

        JournalEntry entry = new JournalEntry();
        entry.title = title;
        entry.text = text;
        entry.date = date;
        entry.location = location;
        entry.imageId = imagePath;

        StorageReference fileRef = storageRef.child(imagePath);

        UploadTask uploadTask = fileRef.putFile(imageUri);

        // Monitor the upload progress
        uploadTask.addOnSuccessListener(taskSnapshot -> {
           Log.d("test", "success image upload");
        }).addOnFailureListener(e -> {
            Log.d("test", "error image upload:" + e.toString());
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                journalDatabase.addEntryToRoomAndFirestore(entry);
            }
        }).start();

        closeFragment();
    }
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        permissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create a file to save the captured image
        File imageFile = null;
        try {
            imageFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a content URI using the FileProvider
        imageUri = FileProvider.getUriForFile(requireContext(), "com.example.myapplication.fileprovider", imageFile);

        // Set the image file as the output for the camera intent
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        // Grant permission to the camera app to write to the URI
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // Launch the camera intent
        cameraLauncher.launch(imageUri);
    }
    private void showDatePicker() {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog and set it as the date picker for the editTextDate
        datePickerDialog = new DatePickerDialog(requireContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Update the editTextDate with the selected date
                String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                editTextDate.setText(selectedDate);
            }
        }, year, month, day);

        // Show the date picker dialog
        datePickerDialog.show();
    }

    private void uploadImage() {
        imagePickerLauncher.launch("image/*");
    }
}