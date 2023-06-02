package com.example.myapplication.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.room.Room;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.R;
import com.example.myapplication.database.JournalEntry;
import com.example.myapplication.fragments.AddJournalEntryFragment;
import com.example.myapplication.fragments.ViewJournalEntryFragment;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private AppDatabase appDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLocale(getCurrentLocalization());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Main Screen");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);

        Button btnRemoveAllEntries = findViewById(R.id.btnRemoveAllEntries);
        btnRemoveAllEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllEntries();
            }
        });

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLocalization();
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignIn(v);
            }
        });

        Button button3= findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignOutClicked(v);
            }
        });


        // Initialize the database
        appDatabase = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "journal_database")
                .fallbackToDestructiveMigration()
                .build();

        appDatabase.syncEntriesWithFirestore();

        setAddJournalListener();
        setViewJournalListener();
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

    private void removeAllEntries() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                appDatabase.journalEntryDao().deleteAllEntries();
            }
        }).start();
    }

    private void setViewJournalListener() {
        Button fabViewJournal = findViewById(R.id.button4);
        fabViewJournal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform the database operation on a background thread using coroutines
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<JournalEntry> allEntries = appDatabase.journalEntryDao().getAllEntries();
                        JournalEntry entry = allEntries.get(0);
                        // Switch back to the main thread to open the ViewJournalEntryFragment
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                openViewJournalEntryFragment(entry);
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private void openAddJournalEntryFragment() {;
        GridLayout mainFrame = findViewById(R.id.mainFrame);

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

    private void openViewJournalEntryFragment(JournalEntry entry) {
        GridLayout mainFrame = findViewById(R.id.mainFrame);

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

    public String getCurrentLocalization() {
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        LocaleList localeList = config.getLocales();
        Locale currentLocale = localeList.get(0);

        return currentLocale.getLanguage();
    }

    public void changeLocalization() {
        String currentLanguage = getCurrentLocalization();
        Log.d("app", currentLanguage);

        if (currentLanguage.equals("en")) {
            setLocale("mk");
        } else if (currentLanguage.equals("mk")) {
            setLocale("en");
        }

        recreate();
    }

    private void setLocale(String languageCode) {
        Log.d("app", "Setting locale to " + languageCode);
        Locale locale = new Locale(languageCode);
        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    appDatabase.syncEntriesWithFirestore();
                }
            });

    public void startSignIn(View v) {
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                    new AuthUI.IdpConfig.FacebookBuilder().build(),
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.AnonymousBuilder().build()))
                .build();

        signInLauncher.launch(signInIntent);
    }
    public void onSignOutClicked(View v) {
        AuthUI.getInstance()
                .signOut(this);
    }
}