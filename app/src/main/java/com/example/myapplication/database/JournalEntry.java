package com.example.myapplication.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class JournalEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "image_id")
    public String imageId;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "location")
    public String location;

    @ColumnInfo(name = "userId")
    public String userId;
}