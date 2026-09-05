package com.app.chao.chaoapp.playback;

import android.content.Context;
import android.content.SharedPreferences;

/** Local playback preferences shared by the online and offline players, never sent to a TV. */
public final class VideoPlaybackSettings {
    public static final int FIT = 0;
    public static final int CROP = 1;
    public static final int STRETCH = 2;
    private final SharedPreferences preferences;

    public VideoPlaybackSettings(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences("video_playback_settings", Context.MODE_PRIVATE);
    }

    public float speed() {
        try { return validSpeed(preferences.getFloat("speed", 1f)); }
        catch (ClassCastException error) { return 1f; }
    }

    public void setSpeed(float speed) {
        preferences.edit().putFloat("speed", validSpeed(speed)).apply();
    }

    private static float validSpeed(float speed) {
        return Float.isNaN(speed) || speed < 0.25f || speed > 2f ? 1f : speed;
    }

    public int aspect() {
        try { return validAspect(preferences.getInt("aspect", FIT)); }
        catch (ClassCastException error) { return FIT; }
    }

    public void setAspect(int aspect) {
        preferences.edit().putInt("aspect", validAspect(aspect)).apply();
    }

    private static int validAspect(int aspect) {
        return aspect >= FIT && aspect <= STRETCH ? aspect : FIT;
    }
}
