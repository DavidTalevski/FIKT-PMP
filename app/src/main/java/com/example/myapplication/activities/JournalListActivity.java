package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

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
import com.example.myapplication.fragments.ViewJournalEntryFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class JournalListActivity extends AppCompatActivity implements  JournalEntryAdapter.OnButtonClickListener {

    private RecyclerView recyclerView;
    private JournalEntryAdapter adapter;
    private List<JournalEntry> journalEntries;


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
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<JournalEntry> entries = AppDatabase.getInstance(getApplicationContext()).journalEntryDao().getAllEntries();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        journalEntries = entries;
                        adapter.setJournalEntries(journalEntries);
                    }
                });
            }
        }).start();
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
        FloatingActionButton fabAddJournalEntry = findViewById(R.id.fabAddJournalEntry);
        fabAddJournalEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddJournalEntryFragment();
            }
        });
    }

    private void openAddJournalEntryFragment() {;
        LinearLayout mainFrame = findViewById(R.id.mainFrame);

        mainFrame.setVisibility(View.GONE);
        // Create an instance of the AddJournalEntryFragment
        AddJournalEntryFragment fragment = new AddJournalEntryFragment(mainFrame);

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

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    public void removeAllEntries(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AppDatabase.getInstance(getApplicationContext()).journalEntryDao().deleteAllEntries();
                adapter.notifyDataSetChanged();
            }
        }).start();
    }
}
