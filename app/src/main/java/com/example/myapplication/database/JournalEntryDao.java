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

    @Query("SELECT * FROM journalentry ORDER BY date ASC")
    List<JournalEntry> getAllEntries();

    @Query("SELECT * FROM journalentry WHERE id = :entryId")
    JournalEntry getEntryById(int entryId);

    @Query("DELETE FROM journalentry")
    void deleteAllEntries();
}