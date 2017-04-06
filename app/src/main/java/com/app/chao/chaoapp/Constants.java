package com.app.chao.chaoapp;

import android.os.Environment;

import java.io.File;

/**
 * Description: Constants
 */
public class Constants {
    //================= PATH ====================
    //public static final String PATH_DATA = App.getInstance().getCacheDir().getAbsolutePath() + File.separator + "data";
    public static final String PATH_CACHE = Environment.getExternalStorageDirectory() + File.separator + "com.app.chao.chaoapp" + File.separator + "HttpCache";

    public static final String PRIMARYCOLOR = "PRIMARYCOLOR";
    public static final String TITLECOLOR = "TITLECOLOR";
    public static final String DISCOVERlASTpAGE = "DISCOVERlASTpAGE";
}
