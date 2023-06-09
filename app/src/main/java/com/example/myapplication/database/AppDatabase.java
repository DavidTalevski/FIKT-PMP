package com.example.myapplication.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
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
        Log.d("Firestore", "SYNC ENTRIES WITH FIRESTORE");
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
                        List<JournalEntry> roomEntries = journalEntryDao().getAllEntries(userId);

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

    public void addEntryToRoomAndFirestore(JournalEntry entry, DatabaseActionCallback callback) {
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

                        if (callback != null) {
                            callback.onSuccess();
                        }

                        // Any additional operations you want to perform on Room data
                    });
                })
                .addOnFailureListener(e -> {
                    Log.d("Firestore", "Error adding entry", e);

                    if (callback != null) {
                        callback.onFailed(e);
                    }
                });
    }

    public void deleteEntryFromRoomAndFirestore(JournalEntry entry, DatabaseActionCallback callback) {
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

                        // Delete image from Firebase Storage
                        deleteImageFromFirebaseStorage(entry.imageId);

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.d("Firestore", "Error deleting entry from Firestore: " + entry.documentId, e);

                        if (callback != null) {
                            callback.onFailed(e);
                        }
                    });
        });
    }

    private void deleteImageFromFirebaseStorage(String imageId) {
            // Get a reference to the Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        // Create a reference to the image file in Firebase Storage
        StorageReference imageRef = storageReference.child(imageId);

        // Delete the image file
        imageRef.delete()
            .addOnSuccessListener(aVoid -> {
                Log.d("FirebaseStorage", "Image deleted from Firebase Storage: " + imageId);
            })
            .addOnFailureListener(e -> {
                Log.d("FirebaseStorage", "Error deleting image from Firebase Storage: " + imageId, e);
            });
    }

    public void deleteAllEntriesFromFirestore(String userId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference collectionReference = firestore.collection("pmp-journal-entries");

        collectionReference.whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                for (DocumentSnapshot document : documents) {
                    firestore.collection("pmp-journal-entries")
                            .document(document.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Entry deleted successfully
                                // Perform any additional actions or update the UI
                            })
                            .addOnFailureListener(e -> {
                                // Error deleting entry
                                Log.d("Firestore", "Error deleting entry: " + e.getMessage());
                            });
                }
            })
            .addOnFailureListener(e -> {
                // Error retrieving documents
                Log.d("Firestore", "Error getting documents: " + e.getMessage());
            });
    }


}