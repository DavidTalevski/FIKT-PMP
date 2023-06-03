package com.example.myapplication.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.database.JournalEntry;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_journal_entry_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.createEntry(view);
    }

    private void createEntry(View view) {
        TextView textViewTitle = view.findViewById(R.id.textViewTitle);
        TextView textViewText = view.findViewById(R.id.textViewText);
        TextView textViewDate = view.findViewById(R.id.textViewDate);
        TextView textViewLocation = view.findViewById(R.id.textViewLocation);
        ImageView imageView = view.findViewById(R.id.imageView);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        if (journalEntry != null) {
            textViewTitle.setText(journalEntry.title);
            textViewText.setText(journalEntry.text);
            textViewDate.setText(journalEntry.date);
            textViewLocation.setText(journalEntry.location);

            // Create a storage reference to the file you want to download
            StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(journalEntry.imageId);

            // Show the progress bar while the image is loading
            progressBar.setVisibility(View.VISIBLE);

            // Get the download URL of the file
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Load the image into the ImageView using Glide
                Glide.with(requireContext())
                        .load(uri)
                        .into(imageView);

                // Hide the progress bar after the image is loaded
                progressBar.setVisibility(View.GONE);
            }).addOnFailureListener(e -> {
                imageView.setVisibility(View.GONE);

                // Hide the progress bar if image loading fails
                progressBar.setVisibility(View.GONE);
            });
        }
    }
}
