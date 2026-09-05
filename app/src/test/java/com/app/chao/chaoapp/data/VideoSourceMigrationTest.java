package com.app.chao.chaoapp.data;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.net.ApiAddressManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class VideoSourceMigrationTest {
    @Test public void v3MigrationKeepsSourceMetadataFavoritesAndProgressWithoutInventingUrls() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "episode-migration-test.db";
        context.deleteDatabase(name);
        try (SQLiteDatabase old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
             InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                     "com.app.chao.chaoapp.data.VideoDatabase/3.json"), StandardCharsets.UTF_8)) {
            JsonObject schema = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("database");
            for (JsonElement element : schema.getAsJsonArray("entities")) {
                JsonObject table = element.getAsJsonObject();
                old.execSQL(table.get("createSql").getAsString().replace("${TABLE_NAME}", table.get("tableName").getAsString()));
            }
            old.execSQL("INSERT INTO video_records (videoKey, sourceId, sourceBaseUrl, videoId, title, videoUrl, episodes, favorite, favoriteAt,"
                    + "lastWatchedAt, lastEpisode, positionMs, durationMs, watchCount) VALUES "
                    + "('api:known:id:42','api:known','https://example.com/api/','42','Known','show_1.mp4',3,1,100,200,2,12000,60000,4),"
                    + "('id:42','legacy',null,'42','Old','https://cdn.example/movie.mp4',0,1,101,201,0,15000,60000,5)");
            old.execSQL("INSERT INTO video_progress VALUES ('api:known:id:42',1,11000,60000),('api:known:id:42',2,12000,60000),"
                    + "('id:42',0,15000,60000)");
            old.setVersion(3);
        }
        VideoDatabase db = Room.databaseBuilder(context, VideoDatabase.class, name)
                .addMigrations(VideoDatabase.MIGRATION_3_4).allowMainThreadQueries().build();
        try {
            assertEquals(2, db.libraryDao().favorites().size());
            VideoRecordEntity known = db.libraryDao().find("api:known:id:42");
            assertEquals("api:known", known.sourceId);
            assertEquals("https://example.com/api/", known.sourceBaseUrl);
            assertNull(known.episodeUrlsJson);
            assertEquals("https://example.com/api/show_2.mp4", known.toVideo().getEpisodeVideo(2));
            assertEquals(4, known.watchCount);
            assertEquals(100, known.favoriteAt);
            assertEquals(11000, db.libraryDao().progress(known.videoKey, 1).positionMs);
            assertEquals(12000, db.libraryDao().progress(known.videoKey, 2).positionMs);
            VideoRecordEntity legacy = db.libraryDao().find("id:42");
            assertEquals(VideoSource.LEGACY, legacy.sourceId);
            assertNull(legacy.episodeUrlsJson);
            assertEquals(15000, db.libraryDao().progress(legacy.videoKey, 0).positionMs);
        } finally { db.close(); context.deleteDatabase(name); }
    }

    @Test public void v2MigrationPreservesAllUnknownSourceKeysAndPerEpisodeProgress() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "source-migration-test.db";
        context.deleteDatabase(name);
        ApiAddressManager.saveBaseUrl("https://current.example/api/");
        try (SQLiteDatabase old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
             InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                     "com.app.chao.chaoapp.data.VideoDatabase/2.json"), StandardCharsets.UTF_8)) {
            JsonObject schema = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("database");
            for (JsonElement element : schema.getAsJsonArray("entities")) {
                JsonObject table = element.getAsJsonObject();
                old.execSQL(table.get("createSql").getAsString().replace("${TABLE_NAME}", table.get("tableName").getAsString()));
            }
            old.execSQL("INSERT INTO video_records (videoKey, videoId, title, videoUrl, episodes, favorite, favoriteAt,"
                    + "lastWatchedAt, lastEpisode, positionMs, durationMs, watchCount) VALUES "
                    + "('id:42','42','Old A','https://cdn.example/a.mp4',3,1,100,200,2,12000,60000,4),"
                    + "('url:/movie.mp4',null,'Old B','https://other-cdn.example/movie.mp4',0,1,101,201,0,15000,60000,5)");
            old.execSQL("INSERT INTO video_progress VALUES ('id:42',1,11000,60000),('id:42',2,12000,60000),"
                    + "('url:/movie.mp4',0,15000,60000)");
            old.execSQL("INSERT INTO search_history VALUES ('previous search',100)");
            old.setVersion(2);
        }
        VideoDatabase db = Room.databaseBuilder(context, VideoDatabase.class, name)
                .addMigrations(VideoDatabase.MIGRATION_2_3, VideoDatabase.MIGRATION_3_4).allowMainThreadQueries().build();
        try {
            assertEquals(2, db.libraryDao().favorites().size());
            for (VideoRecordEntity row : db.libraryDao().favorites()) {
                assertEquals(VideoSource.LEGACY, row.sourceId);
                assertNull(row.sourceBaseUrl);
                assertEquals(row.videoKey, VideoRecordEntity.keyOf(row.toVideo()));
                assertEquals(row.videoUrl, row.toVideo().getVideo());
            }
            assertEquals(4, db.libraryDao().find("id:42").watchCount);
            assertEquals(11000, db.libraryDao().progress("id:42",1).positionMs);
            assertEquals(12000, db.libraryDao().progress("id:42",2).positionMs);
            assertEquals(15000, db.libraryDao().progress("url:/movie.mp4",0).positionMs);
            assertEquals("previous search", db.libraryDao().searches(10).get(0).query);
            VideoRes current = new VideoRes(); current.setId("42"); current.setVideo("movie.mp4");
            current.bindApiSource(ApiAddressManager.getBaseUrl());
            assertNull(db.libraryDao().find(VideoRecordEntity.keyOf(current)));
            db.libraryDao().save(VideoRecordEntity.from(current));
            assertEquals(2, db.libraryDao().favorites().size());
            assertEquals(4, db.libraryDao().find("id:42").watchCount);
        } finally { db.close(); context.deleteDatabase(name); }
    }
}
