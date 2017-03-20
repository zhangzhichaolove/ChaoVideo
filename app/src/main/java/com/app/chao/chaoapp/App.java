package com.app.chao.chaoapp;

import android.app.Application;
import android.content.Context;

/**
 * Created by Chao on 2017/3/13.
 */

public class App extends Application {
    private static Context instance;

    public static Context getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //APPS.init(this);
    }


}
