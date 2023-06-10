package com.example.myapplication.fragments;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.installreferrer.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.DatabaseActionCallback;
import com.example.myapplication.database.JournalEntry;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddJournalEntryFragment extends JournalEntryFragment {

    private EditText editTextTitle;

    private Marker selectedMarker;

    private EditText editTextText;
    private EditText editTextDate;

    private Uri imageUri;
    private ImageView closeImage;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ImageView imageView; // Reference to the ImageView where the selected image will be displayed

    public AddJournalEntryFragment(LinearLayout mainFrame) {
        super(mainFrame);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        journalDatabase = AppDatabase.getInstance(getContext());

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        View rootView = inflater.inflate(R.layout.add_journal_entry_fragment, container, false);

        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        // Find the MapView in the layout
        mapView = rootView.findViewById(R.id.mapView);

        // Set the tile source (e.g., MAPNIK, MAPQUEST, etc.)
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Enable zoom controls
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);

        selectedMarker = new Marker(mapView);

        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                showMarkerOnMap(p);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                showMarkerOnMap(p);
                return false;
            }
        }));

        mapView.getController().setZoom(18);

        requestLocationPermission();

        return rootView;
    }

    private void showMarkerOnMap(GeoPoint location) {
        Log.d("Test", "showMarkerOnMap");
        selectedMarker.setPosition(location);
        mapView.getOverlays().add(selectedMarker);
        mapView.invalidate();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d("Test", "1");
            setupMap();
        } else {
            Log.d("Test", "2");

            // Request location permission using registerForActivityResult
            ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        Log.d("Test", "isGranted" + isGranted);
                        if (isGranted) {
                            setupMap();
                        } else {
                            showDefaultLocationOnMap();
                        }
                    }
            );

            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                showDefaultLocationOnMap();
            }
        }
    }

    private void showDefaultLocationOnMap() {
        Log.d("Test", "showDefaultLocationOnMap");
        GeoPoint defaultLocation = new GeoPoint(41.0317, 21.3347);
        showMarkerOnMap(defaultLocation);
        mapView.getController().setCenter(defaultLocation);
        mapView.invalidate();
    }
    private void setupMap() {
        // Enable zoom controls
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);

        // Get the last known location
        Location lastKnownLocation = getLastKnownLocation();

        // Show the current location on the map
        if (lastKnownLocation != null) {
            showLocationOnMap(lastKnownLocation);
        }
    }

    private Location getLastKnownLocation() {
        Location location = null;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        }
        return location;
    }

    private void showLocationOnMap(@NonNull Location location) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapView.getController().setCenter(geoPoint);

//        Marker marker = new Marker(mapView);
        selectedMarker.setPosition(geoPoint);
        mapView.getOverlays().add(selectedMarker);

        Log.d("Test", "showLocationOnMap");

        mapView.invalidate();
    }

    private File createImageFile() throws IOException {
        // Create a unique filename for the image
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timestamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(imageFileName, ".jpg", storageDir);
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
        Button btnDatePicker = view.findViewById(R.id.btnDatePicker);
        imageView = view.findViewById(R.id.imageView);

        Button btnUploadImage = view.findViewById(R.id.btnUploadImage);
        Button btnCaptureImage = view.findViewById(R.id.btnCaptureImage);
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
                showDateTimePicker();
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

        if (title.isEmpty() || text.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }


        GeoPoint location = selectedMarker.getPosition();

        JournalEntry entry = new JournalEntry();
        entry.title = title;
        entry.text = text;
        entry.date = date;
        entry.latitude = location.getLatitude();
        entry.longitude = location.getLongitude();

        if (imageUri != null) {
            String imagePath = "images/" + imageUri.getLastPathSegment();
            entry.imageId = imagePath;
            StorageReference fileRef = storageRef.child(imagePath);

            UploadTask uploadTask = fileRef.putFile(imageUri);

            // Monitor the upload progress
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Log.d("test", "success image upload");
            }).addOnFailureListener(e -> {
                Log.d("test", "error image upload:" + e.toString());
            });
        }

        new Thread(() -> {
            journalDatabase.addEntryToRoomAndFirestore(entry, new DatabaseActionCallback() {
                @Override
                public void onSuccess() {
                    if (mListener != null) {
                        mListener.onEntryCreated();
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    // Handle failure
                }
            });
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
    private void showDateTimePicker() {
        // Get current date and time
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Create DatePickerDialog and set it as the date picker for the editTextDate
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
            // Update the editTextDate with the selected date
            String selectedDate = selectedDayOfMonth + "/" + (selectedMonth + 1) + "/" + selectedYear;
            editTextDate.setText(selectedDate);

            // Create TimePickerDialog and set it as the time picker for the editTextDate
            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (timePickerView, hourOfDay, minuteOfHour) -> {
                // Update the editTextDate with the selected time
                String selectedTime = hourOfDay + ":" + minuteOfHour;
                editTextDate.append(" " + selectedTime);
            }, hour, minute, false);

            // Show the time picker dialog
            timePickerDialog.show();

        }, year, month, day);

        // Show the date picker dialog
        datePickerDialog.show();
    }


    private void uploadImage() {
        imagePickerLauncher.launch("image/*");
    }
}