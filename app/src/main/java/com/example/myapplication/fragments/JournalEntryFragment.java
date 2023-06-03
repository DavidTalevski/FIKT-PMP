package com.example.myapplication.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.example.myapplication.R;
import com.example.myapplication.database.AppDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class JournalEntryFragment extends Fragment {
    protected AppDatabase journalDatabase;
    private LinearLayout mainFrame;

    protected FirebaseStorage storage;
    protected StorageReference storageRef;

    public JournalEntryFragment(LinearLayout mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        journalDatabase = AppDatabase.getInstance(getContext());

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        return inflater.inflate(R.layout.add_journal_entry_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        Button btnClose = getView().findViewById(R.id.btnClose);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
            }
        });

    }

    public void onDestroyView() {
        super.onDestroyView();
        // Perform any cleanup or actions here before the fragment is closed
        closeFragment();
    }

    protected void closeFragment() {
        mainFrame.setVisibility(View.VISIBLE);

        // Close or remove the fragment from the activity
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}