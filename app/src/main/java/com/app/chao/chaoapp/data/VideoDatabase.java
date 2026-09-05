package com.app.chao.chaoapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {VideoRecordEntity.class, SearchHistoryEntity.class, VideoProgressEntity.class}, version = 4,
        exportSchema = true)
public abstract class VideoDatabase extends RoomDatabase {
    private static volatile VideoDatabase instance;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS video_progress (videoKey TEXT NOT NULL, "
                    + "episode INTEGER NOT NULL, positionMs INTEGER NOT NULL, "
                    + "durationMs INTEGER NOT NULL, PRIMARY KEY(videoKey, episode))");
            db.execSQL("INSERT INTO video_progress (videoKey, episode, positionMs, durationMs) "
                    + "SELECT videoKey, lastEpisode, positionMs, durationMs FROM video_records "
                    + "WHERE lastWatchedAt > 0");
        }
    };

    public abstract VideoLibraryDao libraryDao();

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Old schemas never recorded the API source; a CDN URL cannot reconstruct it.
            // Preserve every key/progress row, keeping legacy records separate from new sources.
            db.execSQL("ALTER TABLE video_records ADD COLUMN sourceId TEXT NOT NULL DEFAULT 'legacy'");
            db.execSQL("ALTER TABLE video_records ADD COLUMN sourceBaseUrl TEXT");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Null means legacy filename inference; never invent explicit URLs during migration.
            db.execSQL("ALTER TABLE video_records ADD COLUMN episodeUrlsJson TEXT");
        }
    };

    public static VideoDatabase get(Context context) {
        if (instance == null) {
            synchronized (VideoDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    VideoDatabase.class, "video-library.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return instance;
    }
}
