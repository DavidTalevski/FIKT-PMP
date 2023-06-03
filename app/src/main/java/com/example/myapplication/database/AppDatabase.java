package com.example.myapplication.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapplication.database.JournalEntry;
import com.example.myapplication.database.JournalEntryDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Database(entities = {JournalEntry.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract JournalEntryDao journalEntryDao();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "journal_database")
                    .fallbackToDestructiveMigration()
                    .build();

            // not sure if this is the best way of doing it
            instance.syncEntriesWithFirestore();
        }
        return instance;
    }

    public void syncEntriesWithFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            CollectionReference collectionReference = firestore.collection("pmp-journal-entries");

            collectionReference.whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            List<JournalEntry> firestoreEntries = querySnapshot.toObjects(JournalEntry.class);

                            new Thread(() -> {
                                // Retrieve Room entries
                                List<JournalEntry> roomEntries = journalEntryDao().getAllEntries();

                                // Compare entries and update Room if needed
                                for (JournalEntry firestoreEntry : firestoreEntries) {
                                    boolean entryExistsInRoom = false;
                                    for (JournalEntry roomEntry : roomEntries) {
                                        if (roomEntry.id == firestoreEntry.id) {
                                            entryExistsInRoom = true;
                                            break;
                                        }
                                    }

                                    if (!entryExistsInRoom) {
                                        // Add the Firestore entry to Room
                                        journalEntryDao().insert(firestoreEntry);
                                    }
                                }

                                // Search for Room entries not present in Firestore
                                for (JournalEntry roomEntry : roomEntries) {
                                    boolean entryExistsInFirestore = false;
                                    for (JournalEntry firestoreEntry : firestoreEntries) {
                                        if (roomEntry.id == firestoreEntry.id) {
                                            entryExistsInFirestore = true;
                                            break;
                                        }
                                    }

                                    if (!entryExistsInFirestore) {
                                        // Add the Room entry to Firestore
                                        collectionReference.add(roomEntry)
                                                .addOnSuccessListener(documentReference ->
                                                        Log.d("Firestore", "Entry added successfully"))
                                                .addOnFailureListener(e ->
                                                        Log.d("Firestore", "Error adding entry", e));
                                    }
                                }
                            }).start();

                        } else {
                            Log.d("Firestore", "Error getting Firestore entries", task.getException());
                        }
                    });
        } else {
            Log.d("Firestore", "User not authenticated. Entries not synchronized.");
        }
    }

    public void addEntryToRoomAndFirestore(JournalEntry entry) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Set the user ID for the entry
            entry.userId = userId;

            // Add entry to Room
            journalEntryDao().insert(entry);

            // Add entry to Firestore
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            CollectionReference collectionReference = firestore.collection("pmp-journal-entries");
            collectionReference.add(entry)
                    .addOnSuccessListener(documentReference -> Log.d("Firestore", "Entry added successfully"))
                    .addOnFailureListener(e -> Log.d("Firestore", "Error adding entry", e));
        } else {
            Log.d("Firestore", "User not authenticated. Entry not added.");
        }
    }
}