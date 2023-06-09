package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface JournalEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(JournalEntry entry);

    @Update
    void update(JournalEntry entry);

    @Delete
    void delete(JournalEntry entry);

    @Query("SELECT * FROM journalentry WHERE userId = :userId ORDER BY date ASC")
    List<JournalEntry> getAllEntries(String userId);


    @Query("SELECT * FROM journalentry WHERE id = :entryId")
    JournalEntry getEntryById(int entryId);

    @Query("SELECT * FROM journalentry WHERE documentId = :documentId LIMIT 1")
    JournalEntry getByDocumentId(String documentId);

    @Query("DELETE FROM journalentry WHERE userId = :userId")
    void deleteAllEntries(String userId);
}