package com.example.myapplication.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapplication.database.JournalEntry;
import com.example.myapplication.database.JournalEntryDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

        if (currentUser == null) {
            Log.d("Firestore", "User not authenticated. Cannot sync entries.");
            return;
        }

        String userId = currentUser.getUid();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference collectionReference = firestore.collection("pmp-journal-entries");

        collectionReference.whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();

                    // Use Executor for background execution
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        // Check if Firestore documents exist in Room
                        for (DocumentSnapshot document : documents) {
                            String documentId = document.getId();

                            // Check if documentId exists in Room
                            if (!entryExistsInRoom(documentId)) {
                                JournalEntry entry = document.toObject(JournalEntry.class);
                                entry.documentId = documentId;
                                entry.userId = userId;

                                // Add entry to Room
                                journalEntryDao().insert(entry);
                                Log.d("Firestore", "Entry added to Room: " + documentId);
                            }
                        }

                        // Check if Room entries exist in Firestore
                        List<JournalEntry> roomEntries = journalEntryDao().getAllEntries();

                        for (JournalEntry roomEntry : roomEntries) {
                            if (!entryExistsInFirestore(roomEntry.documentId, documents)) {
                                // Add entry to Firestore

                                // Journal no longer connected to the same document
                                roomEntry.documentId = null;

                                collectionReference.add(roomEntry)
                                        .addOnSuccessListener(documentReference -> {
                                            // Connect journal to the new document
                                            Executor updateExecutor = Executors.newSingleThreadExecutor();
                                            updateExecutor.execute(() -> {
                                                String newDocumentId = documentReference.getId();
                                                roomEntry.documentId = newDocumentId;
                                                journalEntryDao().update(roomEntry);
                                                Log.d("Firestore", "Entry added to Firestore: " + newDocumentId);
                                            });
                                        })
                                        .addOnFailureListener(e -> Log.d("Firestore", "Error adding entry to Firestore", e));
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> Log.d("Firestore", "Error getting documents from Firestore", e));
    }

    private boolean entryExistsInRoom(String documentId) {
        JournalEntry existingEntry = journalEntryDao().getByDocumentId(documentId);
        return existingEntry != null;
    }

    private boolean entryExistsInFirestore(String documentId, List<DocumentSnapshot> documents) {
        for (DocumentSnapshot document : documents) {
            if (document.getId().equals(documentId)) {
                return true;
            }
        }
        return false;
    }

    public void addEntryToRoomAndFirestore(JournalEntry entry) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser == null) {
            Log.d("Firestore", "User not authenticated. Entry not added.");
            return;
        }

        // Set the user ID for the entry
        entry.userId = currentUser.getUid();

        // Add entry to Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference collectionReference = firestore.collection("pmp-journal-entries");

        collectionReference.add(entry)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();
                    entry.documentId = documentId;

                    AsyncTask.execute(() -> {
                        // Perform database operation on a background thread
                        journalEntryDao().insert(entry);
                        Log.d("Firestore", "Entry added successfully");

                        // Any additional operations you want to perform on Room data
                    });
                })
                .addOnFailureListener(e -> Log.d("Firestore", "Error adding entry", e));
    }

    public void deleteEntryFromRoomAndFirestore(JournalEntry entry) {
        // Delete entry from Room using background thread
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            journalEntryDao().delete(entry);

            // Delete entry from Firestore using background thread
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("pmp-journal-entries")
                    .document(entry.documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Entry deleted from Firestore: " + entry.documentId);
                        // Handle any UI updates or callbacks here
                    })
                    .addOnFailureListener(e -> {
                        Log.d("Firestore", "Error deleting entry from Firestore: " + entry.documentId, e);
                        // Handle any error or failure cases here
                    });
        });
    }

}