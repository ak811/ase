package com.example.searchengine.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class MySharedPreferences {
    private static MySharedPreferences mySharedPreferences;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor prefsEditor;

    public static MySharedPreferences getInstance(Context context) {
        if (mySharedPreferences == null) {
            mySharedPreferences = new MySharedPreferences(context);
        }
        return mySharedPreferences;
    }

    @SuppressLint("CommitPrefEdits")
    private MySharedPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.MY_PREFS_FILE_NAME_STR, Context.MODE_PRIVATE);
        prefsEditor = sharedPreferences.edit();
    }

    public void saveData(String key, String value) {
        prefsEditor.putString(key, value);
        prefsEditor.apply();
    }

    public String getData(String key) {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, Constants.NOT_FOUND_FLAG_STR);
        }
        return Constants.NOT_FOUND_FLAG_STR;
    }

    public void removeData(String key) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.remove(key);
        prefsEditor.apply();
    }

    public void saveDataWithoutApply(String key, String value) {
        prefsEditor.putString(key, value);
    }

    public void applyChanges() {
        prefsEditor.apply();
    }
}