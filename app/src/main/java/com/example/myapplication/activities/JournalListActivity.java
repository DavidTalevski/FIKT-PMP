package com.example.myapplication.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.JournalEntry;
import com.example.myapplication.adapters.JournalEntryAdapter;
import com.example.myapplication.fragments.AddJournalEntryFragment;
import com.example.myapplication.fragments.JournalEntryFragment;
import com.example.myapplication.fragments.ViewJournalEntryFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class JournalListActivity extends AppCompatActivity implements  JournalEntryAdapter.OnButtonClickListener, JournalEntryFragment.JournalUpdateListener {

    private RecyclerView recyclerView;
    private JournalEntryAdapter adapter;
    private List<JournalEntry> journalEntries;

    public void onEntryDeleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("deleted_entry", null);
                Toast.makeText(getApplicationContext(), R.string.succesfully_deleted_entry, Toast.LENGTH_SHORT).show();
            }
        });

        loadJournalEntries();
    }

    public void onEntryCreated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("created_entry", null);
                Toast.makeText(getApplicationContext(), R.string.succesfully_created_entry, Toast.LENGTH_SHORT).show();
            }
        });

        loadJournalEntries();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        recyclerView = findViewById(R.id.recyclerView);
        journalEntries = new ArrayList<>();
        adapter = new JournalEntryAdapter(journalEntries, this);
        adapter.setOnButtonClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadJournalEntries();

        setAddJournalListener();
    }

    public void closeActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Close the current activity
    }

    private void loadJournalEntries() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<JournalEntry> entries = AppDatabase.getInstance(getApplicationContext()).journalEntryDao().getAllEntries(userId);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            journalEntries = entries;
                            adapter.setJournalEntries(journalEntries);
                        }
                    });
                }
            }).start();
        } else {
            // User is not authenticated
            String toastMessage = getString(R.string.user_not_authenticated_message);
            Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
        }
    }


    public void onButtonClicked(JournalEntry entry) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        openViewJournalEntryFragment(entry);
                    }
                });
            }
        }).start();
    }

    private void openViewJournalEntryFragment(JournalEntry entry) {
        LinearLayout mainFrame = findViewById(R.id.mainFrame);

        mainFrame.setVisibility(View.GONE);
        // Create an instance of the AddJournalEntryFragment
        ViewJournalEntryFragment fragment = new ViewJournalEntryFragment(mainFrame, entry);

        fragment.setJournalUpdateListener(this);

        // Get the FragmentManager and begin a fragment transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Replace the content frame with the AddJournalEntryFragment
        fragmentTransaction.replace(R.id.contentFrame, fragment);

        // Add the transaction to the back stack
        fragmentTransaction.addToBackStack(null);

        // Commit the fragment transaction
        fragmentTransaction.commit();
    }

    private void setAddJournalListener() {
        Button fabAddJournalEntry = findViewById(R.id.fabAddJournalEntry);
        fabAddJournalEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddJournalEntryFragment();
            }
        });
    }

    private void openAddJournalEntryFragment() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout mainFrame = findViewById(R.id.mainFrame);

        mainFrame.setVisibility(View.GONE);
        // Create an instance of the AddJournalEntryFragment
        AddJournalEntryFragment fragment = new AddJournalEntryFragment(mainFrame);
        fragment.setJournalUpdateListener(this);

        // Get the FragmentManager and begin a fragment transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Replace the content frame with the AddJournalEntryFragment
        fragmentTransaction.replace(R.id.contentFrame, fragment);

        // Add the transaction to the back stack
        fragmentTransaction.addToBackStack(null);

        // Commit the fragment transaction
        fragmentTransaction.commit();
    }

    public void removeAllEntries(View view) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated_message), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.confirm_deletion_title));
        builder.setMessage(getString(R.string.confirm_deletion_message));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userId = currentUser.getUid();

                // Perform deletion operations in the background
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Delete all entries from Room database
                        AppDatabase.getInstance(getApplicationContext()).journalEntryDao().deleteAllEntries(userId);

                        // Delete all entries from Firestore
                        AppDatabase.getInstance(getApplicationContext()).deleteAllEntriesFromFirestore(userId);

                        // Update the UI or perform any necessary actions
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), getString(R.string.delete_entries_success), Toast.LENGTH_SHORT).show();
                                loadJournalEntries();
                            }
                        });
                    }
                });
            }
        });
        builder.setNegativeButton(getString(R.string.no), null);
        builder.show();
    }
}
