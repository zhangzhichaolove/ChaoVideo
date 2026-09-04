package com.app.chao.chaoapp.net;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.chao.chaoapp.App;

import okhttp3.HttpUrl;

/** Persists and validates the video API base URL. */
public final class ApiAddressManager {
    private static final String PREFERENCES_NAME = "api_address";
    private static final String KEY_VIDEO_API = "video_api";

    private ApiAddressManager() {
    }

    public static String getBaseUrl() {
        return preferences().getString(KEY_VIDEO_API, VideoApis.HOST);
    }

    public static boolean saveBaseUrl(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        preferences().edit().putString(KEY_VIDEO_API, normalized).apply();
        RetrofitHelper.resetVideoApi();
        return true;
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        if (!candidate.endsWith("/")) {
            candidate += "/";
        }
        HttpUrl url = HttpUrl.parse(candidate);
        if (url == null || !("http".equals(url.scheme()) || "https".equals(url.scheme()))) {
            return null;
        }
        return url.toString();
    }

    private static SharedPreferences preferences() {
        Context context = App.getInstance();
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
