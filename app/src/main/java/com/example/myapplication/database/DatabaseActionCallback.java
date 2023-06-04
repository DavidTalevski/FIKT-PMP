package com.example.myapplication.database;

public interface DatabaseActionCallback {
    void onSuccess();
    void onFailed(Exception e);
}