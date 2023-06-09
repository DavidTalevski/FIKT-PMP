package com.example.myapplication.fragments;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.database.AppDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.osmdroid.views.MapView;

public abstract class JournalEntryFragment extends Fragment {

    protected JournalUpdateListener mListener;

    public interface JournalUpdateListener {
        void onEntryDeleted();

        void onEntryCreated();
    }

    public void setJournalUpdateListener(JournalUpdateListener listener) {
        mListener = listener;
    }
    protected AppDatabase journalDatabase;
    private LinearLayout mainFrame;

    protected MapView mapView;

    protected FirebaseStorage storage;
    protected StorageReference storageRef;

    public JournalEntryFragment(LinearLayout mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

//    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

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