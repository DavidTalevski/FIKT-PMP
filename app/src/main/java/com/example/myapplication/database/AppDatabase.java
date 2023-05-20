package com.example.myapplication.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.myapplication.database.JournalEntry;
import com.example.myapplication.database.JournalEntryDao;

@Database(entities = {JournalEntry.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract JournalEntryDao journalEntryDao();
}