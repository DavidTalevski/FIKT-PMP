package com.example.myapplication.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.R;
import com.example.myapplication.database.JournalEntry;
import com.example.myapplication.fragments.ViewJournalEntryFragment;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private AppDatabase appDatabase;

    private ImageView profileImageView;
    private TextView signInTextView;

    private Button signInButton;
    private Button signOutButton;

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

        profileImageView = findViewById(R.id.profileImageView);
        signInTextView = findViewById(R.id.signInTextView);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLocalization();
            }
        });

        signInButton = findViewById(R.id.signInButton);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignIn(v);
            }
        });

        signOutButton= findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSignOutClicked(v);
            }
        });


        appDatabase = AppDatabase.getInstance(this);

    }

    // Function to add signed-in UI elements
    private void addSignedInUI(String userName, Uri profilePictureUri) {
        // Add profile picture
        profileImageView.setVisibility(View.VISIBLE);
        Glide.with(this).load(profilePictureUri).into(profileImageView);

        // Add signed-in text
        signInTextView.setText(getString(R.string.signed_in_as, userName));

    }

    // Function to remove signed-in UI elements
    private void removeSignedInUI() {
        // Remove profile picture
        profileImageView.setVisibility(View.GONE);

        // Remove signed-in text
        signInTextView.setText(R.string.you_are_not_signed_in);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserSignInStatus();
    }

    // Call this method after a successful sign-in
    private void handleSignInSuccess() {
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("user_logged_in", null);
        Toast.makeText(getApplicationContext(), R.string.succesfully_logged_in, Toast.LENGTH_SHORT).show();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userName = user.getDisplayName();
            Uri profilePictureUri = user.getPhotoUrl();
            addSignedInUI(userName, profilePictureUri);
            updateButtonVisibility(true);
            appDatabase.syncEntriesWithFirestore();
        }
    }


    // Call this method after a sign-out
    private void handleSignOut() {
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("user_logged_out", null);

        Toast.makeText(getApplicationContext(), R.string.succesfully_logged_out, Toast.LENGTH_SHORT).show();
        removeSignedInUI();
        updateButtonVisibility(false);
        // Perform any additional actions needed after sign-out
    }

    // Check if the user is signed in and update UI accordingly
// Check if the user is signed in and update UI accordingly
    private void checkUserSignInStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
            String userName = user.getDisplayName();
            Uri profilePictureUri = user.getPhotoUrl();
            addSignedInUI(userName, profilePictureUri);
            updateButtonVisibility(true);
        } else {
            // User is signed out
            removeSignedInUI();
            updateButtonVisibility(false);
        }
    }

    // Function to update the visibility of sign-in and sign-out buttons
    private void updateButtonVisibility(boolean signedIn) {
        if (signedIn) {
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
        } else {
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
        }
    }

    public String getCurrentLocalization() {
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        LocaleList localeList = config.getLocales();
        Locale currentLocale = localeList.get(0);

        return currentLocale.getLanguage();
    }

    public void changeLocalization() {
        FirebaseAnalytics.getInstance(getApplicationContext()).logEvent("localization_changed", null);
        String currentLanguage = getCurrentLocalization();
        Log.d("app", currentLanguage);

        if (currentLanguage.equals("en")) {
            setLocale("mk");
        } else if (currentLanguage.equals("mk")) {
            setLocale("en");
        }


        Toast.makeText(getApplicationContext(), R.string.language_changed_success, Toast.LENGTH_SHORT).show();

        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void setLocale(String languageCode) {
        Log.d("app", "Setting locale to " + languageCode);
        Locale locale = new Locale(languageCode);
        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public void openViewJournalsActivity(View view) {
        Intent intent = new Intent(this, JournalListActivity.class);
        startActivity(intent);
    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    appDatabase.syncEntriesWithFirestore();
                    handleSignInSuccess();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.error_sign_in, Toast.LENGTH_SHORT).show();
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
        handleSignOut();
    }
}