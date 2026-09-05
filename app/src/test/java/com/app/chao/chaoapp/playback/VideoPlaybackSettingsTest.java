package com.app.chao.chaoapp.playback;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class VideoPlaybackSettingsTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private VideoPlaybackSettings settings;

    @Before public void reset() {
        context.getSharedPreferences("video_playback_settings", Context.MODE_PRIVATE).edit().clear().commit();
        settings = new VideoPlaybackSettings(context);
    }

    @Test public void defaultsPreserveOriginalAspectAndNormalSpeed() {
        assertEquals(1f, settings.speed(), 0f);
        assertEquals(VideoPlaybackSettings.FIT, settings.aspect());
    }

    @Test public void speedAndAspectSurviveNewInstancesWithoutOverwritingEachOther() {
        settings.setSpeed(1.75f);
        settings.setAspect(VideoPlaybackSettings.CROP);
        VideoPlaybackSettings reopened = new VideoPlaybackSettings(context);
        assertEquals(1.75f, reopened.speed(), 0f);
        assertEquals(VideoPlaybackSettings.CROP, reopened.aspect());
        reopened.setAspect(VideoPlaybackSettings.STRETCH);
        assertEquals(1.75f, settings.speed(), 0f);
        assertEquals(VideoPlaybackSettings.STRETCH, settings.aspect());
    }

    @Test public void media3BuiltInSpeedRangeIsPreserved() {
        for (float speed : new float[]{0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f}) {
            settings.setSpeed(speed);
            assertEquals(speed, new VideoPlaybackSettings(context).speed(), 0f);
        }
    }

    @Test public void invalidPreferencesFallBackRatherThanBreakingPlayback() {
        for (float speed : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, 10}) {
            settings.setSpeed(speed);
            assertEquals(1f, settings.speed(), 0f);
        }
        settings.setAspect(999);
        assertEquals(VideoPlaybackSettings.FIT, settings.aspect());
        context.getSharedPreferences("video_playback_settings", Context.MODE_PRIVATE).edit()
                .putString("speed", "broken").putString("aspect", "broken").commit();
        assertEquals(1f, settings.speed(), 0f);
        assertEquals(VideoPlaybackSettings.FIT, settings.aspect());
    }
}
