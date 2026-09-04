package com.app.chao.chaoapp;

import android.annotation.SuppressLint;
import android.app.Application;

/**
 * Created by Chao on 2017/3/13.
 */

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }


}
