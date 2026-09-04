package com.app.chao.chaoapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {VideoRecordEntity.class, SearchHistoryEntity.class}, version = 1,
        exportSchema = false)
public abstract class VideoDatabase extends RoomDatabase {
    private static volatile VideoDatabase instance;

    public abstract VideoLibraryDao libraryDao();

    public static VideoDatabase get(Context context) {
        if (instance == null) {
            synchronized (VideoDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    VideoDatabase.class, "video-library.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
