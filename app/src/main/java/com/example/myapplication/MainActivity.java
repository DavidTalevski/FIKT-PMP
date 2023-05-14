package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;

import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLocale(getCurrentLocalization());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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