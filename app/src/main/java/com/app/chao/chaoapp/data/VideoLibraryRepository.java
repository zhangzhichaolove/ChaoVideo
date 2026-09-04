package com.app.chao.chaoapp.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.app.chao.chaoapp.bean.VideoRes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VideoLibraryRepository {
    private static volatile VideoLibraryRepository instance;
    private final VideoLibraryDao dao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VideoLibraryRepository(Context context) {
        dao = VideoDatabase.get(context).libraryDao();
    }

    public static VideoLibraryRepository get(Context context) {
        if (instance == null) {
            synchronized (VideoLibraryRepository.class) {
                if (instance == null) {
                    instance = new VideoLibraryRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void recordOpened(VideoRes video, int episode) {
        databaseExecutor.execute(() -> {
            VideoRecordEntity record = mergedRecord(video);
            record.lastWatchedAt = System.currentTimeMillis();
            record.lastEpisode = Math.max(episode, record.lastEpisode);
            record.watchCount++;
            dao.save(record);
        });
    }

    public void updateProgress(VideoRes video, int episode, long positionMs, long durationMs) {
        databaseExecutor.execute(() -> {
            VideoRecordEntity record = mergedRecord(video);
            record.lastWatchedAt = System.currentTimeMillis();
            record.lastEpisode = Math.max(0, episode);
            record.positionMs = Math.max(0, positionMs);
            record.durationMs = Math.max(0, durationMs);
            dao.save(record);
        });
    }

    public void toggleFavorite(VideoRes video, ValueCallback<Boolean> callback) {
        databaseExecutor.execute(() -> {
            VideoRecordEntity record = mergedRecord(video);
            record.favorite = !record.favorite;
            record.favoriteAt = record.favorite ? System.currentTimeMillis() : 0;
            dao.save(record);
            post(callback, record.favorite);
        });
    }

    public void isFavorite(VideoRes video, ValueCallback<Boolean> callback) {
        databaseExecutor.execute(() -> {
            VideoRecordEntity record = dao.find(VideoRecordEntity.keyOf(video));
            post(callback, record != null && record.favorite);
        });
    }

    public void loadFavorites(ValueCallback<List<VideoRes>> callback) {
        loadRecords(dao::favorites, callback);
    }

    public void loadHistory(ValueCallback<List<VideoRes>> callback) {
        loadRecords(dao::history, callback);
    }

    public void clearHistory(Runnable callback) {
        databaseExecutor.execute(() -> {
            dao.deleteUnfavoritedHistory();
            dao.clearFavoriteHistory();
            post(callback);
        });
    }

    public void addSearch(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return;
        }
        databaseExecutor.execute(() -> {
            SearchHistoryEntity search = new SearchHistoryEntity();
            search.query = normalized;
            search.searchedAt = System.currentTimeMillis();
            dao.saveSearch(search);
        });
    }

    public void loadSearches(ValueCallback<List<String>> callback) {
        databaseExecutor.execute(() -> {
            List<String> result = new ArrayList<>();
            for (SearchHistoryEntity search : dao.searches(20)) {
                result.add(search.query);
            }
            post(callback, result);
        });
    }

    public void clearSearches(Runnable callback) {
        databaseExecutor.execute(() -> {
            dao.clearSearches();
            post(callback);
        });
    }

    private VideoRecordEntity mergedRecord(VideoRes video) {
        VideoRecordEntity metadata = VideoRecordEntity.from(video);
        VideoRecordEntity saved = dao.find(metadata.videoKey);
        if (saved == null) {
            return metadata;
        }
        saved.mergeMetadata(metadata);
        return saved;
    }

    private void loadRecords(RecordLoader loader, ValueCallback<List<VideoRes>> callback) {
        databaseExecutor.execute(() -> {
            List<VideoRes> result = new ArrayList<>();
            for (VideoRecordEntity record : loader.load()) {
                result.add(record.toVideo());
            }
            post(callback, result);
        });
    }

    private <T> void post(ValueCallback<T> callback, T value) {
        if (callback != null) {
            mainHandler.post(() -> callback.onResult(value));
        }
    }

    private void post(Runnable callback) {
        if (callback != null) {
            mainHandler.post(callback);
        }
    }

    private interface RecordLoader {
        List<VideoRecordEntity> load();
    }

    public interface ValueCallback<T> {
        void onResult(T value);
    }
}
