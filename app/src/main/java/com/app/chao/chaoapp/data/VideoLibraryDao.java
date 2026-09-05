package com.app.chao.chaoapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VideoLibraryDao {
    @Query("SELECT * FROM video_records WHERE videoKey = :key LIMIT 1")
    VideoRecordEntity find(String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(VideoRecordEntity record);

    @Query("SELECT * FROM video_progress WHERE videoKey = :key AND episode = :episode LIMIT 1")
    VideoProgressEntity progress(String key, int episode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveProgress(VideoProgressEntity progress);

    @Query("DELETE FROM video_progress")
    void clearProgress();

    @Query("SELECT * FROM video_records WHERE favorite = 1 ORDER BY favoriteAt DESC")
    List<VideoRecordEntity> favorites();

    @Query("SELECT * FROM video_records WHERE lastWatchedAt > 0 ORDER BY lastWatchedAt DESC")
    List<VideoRecordEntity> history();

    @Query("DELETE FROM video_records WHERE lastWatchedAt > 0 AND favorite = 0")
    void deleteUnfavoritedHistory();

    @Query("UPDATE video_records SET lastWatchedAt = 0, lastEpisode = 0, positionMs = 0, durationMs = 0, watchCount = 0 WHERE lastWatchedAt > 0 AND favorite = 1")
    void clearFavoriteHistory();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSearch(SearchHistoryEntity search);

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    List<SearchHistoryEntity> searches(int limit);

    @Query("DELETE FROM search_history")
    void clearSearches();
}
