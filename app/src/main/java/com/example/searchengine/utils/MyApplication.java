package com.example.searchengine.utils;

import android.app.Application;

public class MyApplication extends Application {

    public static com.example.searchengine.utils.MySharedPreferences mySharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        mySharedPreferences = com.example.searchengine.utils.MySharedPreferences.getInstance(this);
    }
}
