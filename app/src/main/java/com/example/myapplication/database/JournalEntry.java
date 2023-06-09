package com.example.myapplication.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class JournalEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "documentId")
    public String documentId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "image_id")
    public String imageId;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "userId")
    public String userId;
}