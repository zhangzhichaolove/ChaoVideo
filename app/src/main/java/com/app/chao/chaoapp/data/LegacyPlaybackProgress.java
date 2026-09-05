package com.app.chao.chaoapp.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.annotation.SuppressLint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class LegacyPlaybackProgress {
    private static final String PREFERENCES = "video_playback_progress";
    private final SharedPreferences preferences;

    LegacyPlaybackProgress(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    long getPosition(String videoUrl) {
        if (videoUrl == null) {
            return 0;
        }
        return preferences.getLong(key(videoUrl), 0);
    }

    @SuppressLint("ApplySharedPref") // This database-executor barrier must precede clearing Room history.
    void clear() {
        // Called on the database executor before clearing Room history.
        preferences.edit().clear().commit();
    }

    int getLastEpisode(String videoUrl, int episodeCount) {
        if (videoUrl == null || episodeCount <= 0) {
            return 0;
        }
        int saved = preferences.getInt("episode_" + key(videoUrl), 1);
        return Math.max(1, Math.min(saved, episodeCount));
    }

    private static String key(String videoUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(videoUrl.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format(java.util.Locale.US, "%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(videoUrl.hashCode());
        }
    }
}
