package com.app.chao.chaoapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/** One authoritative resume position per movie/episode. A zero position means start over. */
@Entity(tableName = "video_progress", primaryKeys = {"videoKey", "episode"})
public class VideoProgressEntity {
    @NonNull public String videoKey = "";
    public int episode;
    public long positionMs;
    public long durationMs;

    public static long resumePosition(long positionMs, long durationMs) {
        long position = Math.max(0, positionMs);
        return durationMs > 0 && position >= Math.max(0, durationMs - 1000) ? 0 : position;
    }
}
