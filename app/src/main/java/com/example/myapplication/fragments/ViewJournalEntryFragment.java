package com.example.myapplication.fragments;

import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.R;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.DatabaseActionCallback;
import com.example.myapplication.database.JournalEntry;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.osmdroid.config.Configuration;
import com.android.installreferrer.BuildConfig;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;


public class ViewJournalEntryFragment extends JournalEntryFragment {

    private static final String ARG_JOURNAL_ENTRY = "journal_entry";

    private JournalEntry journalEntry;

    public ViewJournalEntryFragment(LinearLayout mainFrame , JournalEntry entry) {
        super(mainFrame);
        journalEntry = entry;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            journalEntry = getArguments().getParcelable(ARG_JOURNAL_ENTRY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        journalDatabase = AppDatabase.getInstance(getContext());

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        View rootView = inflater.inflate(R.layout.view_journal_entry_fragment, container, false);

        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        // Find the MapView in the layout
        mapView = rootView.findViewById(R.id.mapView);

        // Set the tile source (e.g., MAPNIK, MAPQUEST, etc.)
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Enable zoom controls
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);

        return rootView;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.createEntry(view);

        Button btnDelete = view.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirmation_title))
                    .setMessage(getString(R.string.confirmation_message))
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        // User confirmed, proceed with deletion
                        AppDatabase.getInstance(getContext()).deleteEntryFromRoomAndFirestore(journalEntry, new DatabaseActionCallback() {
                            @Override
                            public void onSuccess() {
                                if (mListener != null) {
                                    mListener.onEntryDeleted();
                                }
                            }

                            @Override
                            public void onFailed(Exception e) {
                                // Handle failure
                            }
                        });

                        closeFragment();
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        });


    }

    private void createEntry(View view) {
        TextView textViewTitle = view.findViewById(R.id.textViewTitle);
        TextView textViewText = view.findViewById(R.id.textViewText);
        TextView textViewDate = view.findViewById(R.id.textViewDate);
        ImageView imageView = view.findViewById(R.id.imageView);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        mapView = view.findViewById(R.id.mapView);

        if (journalEntry != null) {
            textViewTitle.setText(journalEntry.title);
            textViewText.setText(journalEntry.text);
            textViewDate.setText(journalEntry.date);

            // Show the latitude and longitude coordinates on the mapView
            double latitude = journalEntry.latitude;
            double longitude = journalEntry.longitude;
            GeoPoint location = new GeoPoint(latitude, longitude);
            mapView.getController().setCenter(location);
            mapView.getController().setZoom(18); // Set an appropriate zoom level

            // Create a marker at the specified coordinates
            Marker marker = new Marker(mapView);
            marker.setPosition(location);
            mapView.getOverlays().add(marker);

            // Create a storage reference to the file you want to download
            if (journalEntry.imageId != null) {
                StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(journalEntry.imageId);

                // Show the progress bar while the image is loading
                progressBar.setVisibility(View.VISIBLE);

                // Get the download URL of the file
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Load the image into the ImageView using Glide
                    Glide.with(requireContext())
                            .load(uri)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    // Hide the progress bar if image loading fails
                                    progressBar.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    // Image loading is successful, hide the progress bar
                                    progressBar.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(imageView);
                }).addOnFailureListener(e -> {
                    imageView.setVisibility(View.GONE);

                    // Hide the progress bar if image loading fails
                    progressBar.setVisibility(View.GONE);
                });
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

}
