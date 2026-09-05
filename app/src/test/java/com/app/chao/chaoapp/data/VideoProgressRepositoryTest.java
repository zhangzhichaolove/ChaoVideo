package com.app.chao.chaoapp.data;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Looper;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;

import com.app.chao.chaoapp.bean.VideoRes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class VideoProgressRepositoryTest {
    private Context context;
    private VideoDatabase database;
    private VideoLibraryRepository repository;
    private ExecutorService executor;
    private VideoRes video;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("video_playback_progress", Context.MODE_PRIVATE).edit().clear().commit();
        database = Room.inMemoryDatabaseBuilder(context, VideoDatabase.class).allowMainThreadQueries().build();
        repository = new VideoLibraryRepository(context, database);
        executor = ReflectionHelpers.getField(repository, "databaseExecutor");
        video = new VideoRes();
        video.setId("series");
        video.setTitle("Series");
        video.setVideo("https://example.com/series_1.mp4");
        video.setEpisodes(3);
    }

    @After public void tearDown() throws Exception {
        drain();
        executor.shutdownNow();
        database.close();
    }

    private void drain() throws Exception {
        executor.submit(() -> { }).get(10, TimeUnit.SECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private VideoProgressEntity load(int episode) throws Exception {
        AtomicReference<VideoProgressEntity> result = new AtomicReference<>();
        repository.loadProgress(video, episode, result::set);
        drain();
        return result.get();
    }

    @Test public void eachEpisodeHasIndependentProgressAndZeroResetsPosition() throws Exception {
        repository.updateProgress(video, 1, 12000, 60000);
        repository.updateProgress(video, 2, 23000, 60000);
        assertEquals(12000, load(1).positionMs);
        assertEquals(23000, load(2).positionMs);
        repository.updateProgress(video, 1, 0, 60000);
        assertEquals(0, load(1).positionMs);
        assertEquals(23000, load(2).positionMs);
    }

    @Test public void completionDoesNotRestoreTheEndOfTheMovie() throws Exception {
        repository.updateProgress(video, 1, 60000, 60000);
        assertEquals(0, load(1).positionMs);
        assertEquals(0, VideoProgressEntity.resumePosition(-100, 0));
    }

    @Test public void explicitEpisodeUrlsSurviveFavoritesAndRefreshWithoutResettingProgress() throws Exception {
        video.bindApiSource("https://example.com/api/");
        video.setEpisodeUrls(new String[]{"first.mkv?token=old", "last.mkv"});
        repository.toggleFavorite(video, null);
        repository.updateProgress(video, 1, 12000, 60000);
        repository.updateProgress(video, 2, 23000, 60000);
        drain();
        String key = VideoRecordEntity.keyOf(video);
        VideoRes restored = database.libraryDao().find(key).toVideo();
        assertEquals(2, restored.getEpisodes());
        assertEquals("https://example.com/api/first.mkv?token=old", restored.getEpisodeVideo(1));
        video.setEpisodeUrls(new String[]{"first.mkv?token=new", "last.mkv", "bonus.mkv"});
        repository.recordOpened(video, 2);
        drain();
        VideoRecordEntity saved = database.libraryDao().find(key);
        assertTrue(saved.favorite);
        assertEquals(3, saved.toVideo().getEpisodes());
        assertEquals("https://example.com/api/first.mkv?token=new", saved.toVideo().getEpisodeVideo(1));
        assertEquals(12000, database.libraryDao().progress(key, 1).positionMs);
        assertEquals(23000, database.libraryDao().progress(key, 2).positionMs);
    }

    @Test public void historyClearRemovesLegacyAndRoomPositionsButKeepsFavorites() throws Exception {
        String legacyKey = ReflectionHelpers.callStaticMethod(LegacyPlaybackProgress.class, "key",
                ClassParameter.from(String.class, video.getEpisodeVideo(3)));
        context.getSharedPreferences("video_playback_progress", Context.MODE_PRIVATE).edit()
                .putLong(legacyKey, 34000).putInt("episode_" + legacyKey, 3).commit();
        assertEquals(34000, load(3).positionMs);
        repository.toggleFavorite(video, null);
        repository.updateProgress(video, 1, 12000, 60000);
        repository.clearHistory(null);
        drain();
        assertTrue(database.libraryDao().history().isEmpty());
        assertEquals(1, database.libraryDao().favorites().size());
        assertEquals(0, load(1).positionMs);
        assertEquals(0, load(3).positionMs);
        assertTrue(context.getSharedPreferences("video_playback_progress", Context.MODE_PRIVATE)
                .getAll().isEmpty());
    }

    @Test public void sameIdInTwoSourcesKeepsFavoritesAndEpisodeProgressIndependent() throws Exception {
        VideoRes a = new VideoRes(); a.setId("42"); a.setVideo("https://cdn.example/shared.mp4");
        a.bindApiSource("https://api.example/a/");
        VideoRes b = new VideoRes(); b.setId("42"); b.setVideo("https://cdn.example/shared.mp4");
        b.bindApiSource("https://api.example/b/");
        repository.toggleFavorite(a, null);
        repository.toggleFavorite(b, null);
        repository.updateProgress(a, 1, 12000, 60000);
        repository.updateProgress(b, 1, 23000, 60000);
        drain();
        assertEquals(2, database.libraryDao().favorites().size());
        assertEquals(12000, database.libraryDao().progress(VideoRecordEntity.keyOf(a), 1).positionMs);
        assertEquals(23000, database.libraryDao().progress(VideoRecordEntity.keyOf(b), 1).positionMs);
        VideoRes restoredA = database.libraryDao().find(VideoRecordEntity.keyOf(a)).toVideo();
        assertEquals(a.getSourceBaseUrl(), restoredA.getSourceBaseUrl());
        repository.toggleFavorite(restoredA, null);
        drain();
        assertEquals(VideoRecordEntity.keyOf(b), database.libraryDao().favorites().get(0).videoKey);
        assertEquals(2, database.libraryDao().history().size());
    }

    @Test public void refreshedSignedUrlUpdatesMetadataWithoutLosingFavoriteOrProgress() throws Exception {
        video.bindApiSource("https://api.example/a/");
        repository.toggleFavorite(video, null);
        repository.updateProgress(video, 1, 12000, 60000);
        drain();
        VideoRes refreshed = new VideoRes(); refreshed.setId(video.getId());
        refreshed.setVideo("https://cdn.example/updated.mp4?signature=new");
        refreshed.bindApiSource(video.getSourceBaseUrl());
        repository.recordOpened(refreshed, 1);
        drain();
        VideoRecordEntity row = database.libraryDao().find(VideoRecordEntity.keyOf(refreshed));
        assertTrue(row.favorite);
        assertEquals(refreshed.getVideo(), row.videoUrl);
        assertEquals(12000, database.libraryDao().progress(row.videoKey, 1).positionMs);
    }

    @Test public void unattributedOldUrlProgressIsNotImportedIntoANewSource() throws Exception {
        String legacyKey = ReflectionHelpers.callStaticMethod(LegacyPlaybackProgress.class, "key",
                ClassParameter.from(String.class, video.getEpisodeVideo(3)));
        context.getSharedPreferences("video_playback_progress", Context.MODE_PRIVATE).edit()
                .putLong(legacyKey, 34000).commit();
        video.bindApiSource("https://api.example/a/");
        assertEquals(0, load(3).positionMs);
        assertEquals(34000, context.getSharedPreferences("video_playback_progress", Context.MODE_PRIVATE)
                .getLong(legacyKey, 0));
    }

    @Test public void versionOneUpgradeKeepsFavoritesAndImportsLastProgress() throws Exception {
        String name = "migration-test.db";
        context.deleteDatabase(name);
        try (SQLiteDatabase old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
             InputStreamReader reader = new InputStreamReader(getClass().getClassLoader()
                     .getResourceAsStream("com.app.chao.chaoapp.data.VideoDatabase/1.json"),
                     StandardCharsets.UTF_8)) {
            JsonObject schema = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("database");
            for (JsonElement element : schema.getAsJsonArray("entities")) {
                JsonObject entity = element.getAsJsonObject();
                old.execSQL(entity.get("createSql").getAsString().replace("${TABLE_NAME}",
                        entity.get("tableName").getAsString()));
            }
            old.execSQL("INSERT INTO video_records (videoKey,title,episodes,favorite,favoriteAt,"
                    + "lastWatchedAt,lastEpisode,positionMs,durationMs,watchCount) "
                    + "VALUES ('id:old','Old favorite',3,1,100,200,2,12000,60000,4)");
            old.setVersion(1);
        }
        VideoDatabase upgraded = Room.databaseBuilder(context, VideoDatabase.class, name)
                .addMigrations(VideoDatabase.MIGRATION_1_2, VideoDatabase.MIGRATION_2_3, VideoDatabase.MIGRATION_3_4).allowMainThreadQueries().build();
        try {
            assertEquals("Old favorite", upgraded.libraryDao().favorites().get(0).title);
            assertEquals(12000, upgraded.libraryDao().progress("id:old", 2).positionMs);
        } finally {
            upgraded.close();
        }
        context.deleteDatabase(name);
    }
}
