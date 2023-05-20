package com.example.myapplication.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.R;
import com.example.myapplication.database.JournalEntry;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLocale(getCurrentLocalization());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the database
        AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "journal_database")
                .build();

        // Create journal entries
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Create some sample entries
                JournalEntry entry1 = new JournalEntry();
                entry1.title = "Entry 1";
                entry1.text = "This is the first entry.";
                entry1.date = "2023-05-20";
                appDatabase.journalEntryDao().insert(entry1);

                JournalEntry entry2 = new JournalEntry();
                entry2.title = "Entry 2";
                entry2.text = "This is the second entry.";
                entry2.date = "2023-05-21";
                appDatabase.journalEntryDao().insert(entry2);

                // Retrieve all entries from the database
                List<JournalEntry> allEntries = appDatabase.journalEntryDao().getAllEntries();

                // Log the retrieved entries
                for (JournalEntry entry : allEntries) {
                    Log.d("MainActivity", "Title: " + entry.title + ", Text: " + entry.text);
                }
            }
        });
    }

    public void onChangeLocalizationButtonClicked(View v) {
        changeLocalization();
    }

    public String getCurrentLocalization() {
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        LocaleList localeList = config.getLocales();
        Locale currentLocale = localeList.get(0);
        String currentLanguage = currentLocale.getLanguage();

        return currentLanguage;
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

    private ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            (result) -> {
                Log.d("asd", result.toString());
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