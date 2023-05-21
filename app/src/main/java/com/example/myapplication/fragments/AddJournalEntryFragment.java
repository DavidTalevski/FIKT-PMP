package com.example.myapplication.fragments;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

import java.util.Calendar;

public class AddJournalEntryFragment extends Fragment {

    private EditText editTextTitle;
    private EditText editTextText;
    private EditText editTextDate;
    private EditText editTextLocation;
    private Button btnDatePicker;
    private Button btnSelectLocation;
    private Button btnUploadImage;
    private ImageButton btnClose;

    private DatePickerDialog datePickerDialog;

    private GridLayout mainFrame;

    public AddJournalEntryFragment(GridLayout mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.add_journal_entry_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the views
        editTextTitle = view.findViewById(R.id.editTextTitle);
        editTextText = view.findViewById(R.id.editTextText);
        editTextDate = view.findViewById(R.id.editTextDate);
        editTextLocation = view.findViewById(R.id.editTextLocation);
        btnDatePicker = view.findViewById(R.id.btnDatePicker);
        btnSelectLocation = view.findViewById(R.id.btnSelectLocation);
        btnUploadImage = view.findViewById(R.id.btnUploadImage);
        btnClose = view.findViewById(R.id.btnClose);

        // Set click listeners
        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnSelectLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLocation();
            }
        });

        btnUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
            }
        });
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

    private void selectLocation() {
    }

    private void uploadImage() {
    }

    private void closeFragment() {
        mainFrame.setVisibility(View.VISIBLE);

        // Close or remove the fragment from the activity
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}