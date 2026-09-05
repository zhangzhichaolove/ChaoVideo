package com.app.chao.chaoapp.playback;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.download.FixtureServer;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;

@RunWith(AndroidJUnit4.class)
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class OnlineSubtitleExperienceTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void trackSelectionFullscreenPauseAndPipUseActualDecoder() throws Exception {
        try (FixtureServer server = new FixtureServer("subtitle-fixtures")) {
            VideoRes video = video(server);
            try (ActivityScenario<GSYVVideoActivity> page = ActivityScenario.launch(intent(video))) {
                await(page, a -> player(a).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                page.onActivity(a -> { player(a).onVideoPause(); kernel().seekTo(2000); });
                await(page, a -> text(a).contains("English subtitle"));
                page.onActivity(a -> assertCaptionDrawn(a, true));
                capture("embedded-english");
                // Optional reproducer for the documented Media3 MKV seek/preroll limitation.
                // Keep the French assertion below: this probe currently FAILS, not a supported-path check.
                if ("true".equals(InstrumentationRegistry.getArguments().getString("probe_mkv_preroll"))) {
                    page.onActivity(a -> kernel().seekTo(10000));
                    await(page, a -> text(a).isEmpty());
                    page.onActivity(a -> kernel().seekTo(22000));
                    await(page, a -> text(a).contains("ending subtitle"));
                }
                page.onActivity(a -> {
                    kernel().seekTo(2000);
                    Toolbar toolbar = a.findViewById(R.id.toolbar); a.onPrepareOptionsMenu(toolbar.getMenu());
                    assertTrue(toolbar.getMenu().findItem(R.id.action_subtitles).isVisible());
                    toolbar.getMenu().performIdentifierAction(R.id.action_subtitles, 0); choose(a, 3); // French, not a track index from another media.
                });
                await(page, a -> text(a).contains("français"));
                page.onActivity(a -> player(a).getFullscreenButton().performClick());
                await(page, a -> player(a).isIfCurrentIsFullscreen() && text(a).contains("français"));
                page.onActivity(a -> {
                    assertFalse(kernel().isPlaying());
                    assertEquals(2000, kernel().getCurrentPosition(), 200);
                    assertCaptionDrawn(a, true);
                });
                capture("fullscreen-french");
                page.onActivity(a -> {
                    player(a).findViewWithTag("playback_settings").performClick(); choose(a, 2); // No multiple audio tracks: subtitle submenu follows aspect.
                    choose(a, 0); // Off.
                });
                await(page, a -> text(a).isEmpty());
                page.onActivity(a -> assertCaptionDrawn(a, false));
                page.moveToState(Lifecycle.State.CREATED); page.moveToState(Lifecycle.State.RESUMED);
                page.onActivity(a -> assertFalse(kernel().isPlaying()));
                page.onActivity(a -> {
                    player(a).findViewWithTag("playback_settings").performClick(); choose(a, 2); choose(a, 3);
                });
                await(page, a -> text(a).contains("français"));
                page.onActivity(a -> assertTrue(GSYVideoManager.backFromWindowFull(a)));
                await(page, a -> !player(a).isIfCurrentIsFullscreen() && text(a).contains("français"));
                page.onActivity(a -> {
                    player(a).onVideoResume(false);
                    ((Toolbar) a.findViewById(R.id.toolbar)).getMenu().performIdentifierAction(R.id.action_picture_in_picture, 0);
                });
                await(page, a -> a.isInPictureInPictureMode() && a.findViewById(R.id.toolbar).getVisibility() == View.GONE
                        && !player(a).getStartButton().isShown() && text(a).contains("français"));
                page.onActivity(a -> {
                    assertCaptionDrawn(a, true);
                    assertFalse("PiP configuration must not create a fullscreen clone", player(a).isIfCurrentIsFullscreen());
                    assertPipControlsHidden(a);
                });
                context.sendBroadcast(new Intent("com.app.chao.chaoapp.action.PIP_PLAY_PAUSE").setPackage(context.getPackageName()));
                await(page, a -> player(a).getCurrentState() == GSYVideoView.CURRENT_STATE_PAUSE);
                page.onActivity(a -> { assertCaptionDrawn(a, true); assertPipControlsHidden(a); });
                capture("pip-french");
            }
        }
    }

    @Test public void decoderUsesCueTimesAndClearsSeekGaps() throws Exception {
        try (FixtureServer server = new FixtureServer("subtitle-fixtures")) {
            try (ActivityScenario<GSYVVideoActivity> page = ActivityScenario.launch(intent(video(server)))) {
                await(page, a -> player(a).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                page.onActivity(a -> { player(a).onVideoPause(); kernel().seekTo(2000); });
                await(page, a -> text(a).contains("English subtitle"));
                page.onActivity(a -> assertCaptionDrawn(a, true));
                page.onActivity(a -> kernel().seekTo(10000));
                await(page, a -> text(a).isEmpty());
                page.onActivity(a -> assertCaptionDrawn(a, false));
                page.onActivity(a -> kernel().seekTo(22000));
                await(page, a -> text(a).contains("ending subtitle"));
                page.onActivity(a -> { assertCaptionDrawn(a, true); assertFalse(kernel().isPlaying()); });
                capture("timed-ending");
            }
        }
    }

    @Test public void changingEpisodeClearsPreviousSubtitlesAndHidesMissingTrackEntry() throws Exception {
        try (FixtureServer server = new FixtureServer("subtitle-fixtures")) {
            VideoRes video = video(server); video.setEpisodeUrls(new String[]{server.url("dual.mkv"), server.url("plain.mp4")});
            try (ActivityScenario<GSYVVideoActivity> page = ActivityScenario.launch(intent(video))) {
                await(page, a -> player(a).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                page.onActivity(a -> { player(a).onVideoPause(); kernel().seekTo(2000); });
                await(page, a -> !text(a).isEmpty());
                page.onActivity(a -> a.onEpisodeSelected(2));
                await(page, a -> kernel().getDataSource().endsWith("plain.mp4") && player(a).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                page.onActivity(a -> {
                    assertTrue(text(a).isEmpty());
                    Toolbar toolbar = a.findViewById(R.id.toolbar); a.onPrepareOptionsMenu(toolbar.getMenu());
                    assertFalse(toolbar.getMenu().findItem(R.id.action_subtitles).isVisible());
                    player(a).getFullscreenButton().performClick();
                });
                await(page, a -> player(a).isIfCurrentIsFullscreen());
                page.onActivity(a -> {
                    player(a).findViewWithTag("playback_settings").performClick();
                    assertEquals(2, dialog(a).getListView().getCount());
                    dialog(a).dismiss();
                });
                capture("no-subtitle-episode");
            }
        }
    }

    private static void assertPipControlsHidden(GSYVVideoActivity activity) {
        assertFalse(player(activity).getStartButton().isShown());
        assertFalse(player(activity).findViewById(com.shuyu.gsyvideoplayer.R.id.layout_top).isShown());
        assertFalse(player(activity).findViewById(com.shuyu.gsyvideoplayer.R.id.layout_bottom).isShown());
    }

    private static void assertCaptionDrawn(GSYVVideoActivity activity, boolean expected) {
        View view = player(activity).findViewById(R.id.online_subtitles);
        assertTrue(view.isShown()); assertTrue(view.getWidth() > 0 && view.getHeight() > 0);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new android.graphics.Canvas(bitmap));
        int drawn = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) for (int x = 0; x < bitmap.getWidth(); x++) {
            if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 0) drawn++;
        }
        bitmap.recycle();
        if (expected) assertTrue("Subtitle canvas must draw visible pixels, not just an accessibility label", drawn > 20);
        else assertEquals("No stale subtitle pixels", 0, drawn);
    }

    private VideoRes video(FixtureServer server) {
        VideoRes video = new VideoRes(); video.setId("subtitle-fixture-" + System.nanoTime());
        video.setTitle("Online subtitle fixture"); video.setVideo(server.url("dual.mkv")); return video;
    }
    private Intent intent(VideoRes video) { return new Intent(context, GSYVVideoActivity.class).putExtra("videoInfo", video); }
    private static PlaybackVideoPlayer player(GSYVVideoActivity a) { return (PlaybackVideoPlayer) ((PlaybackVideoPlayer) a.findViewById(R.id.detail_player)).getCurrentPlayer(); }
    private static IjkExo2MediaPlayer kernel() { return (IjkExo2MediaPlayer) GSYVideoManager.instance().getCurPlayerManager().getMediaPlayer(); }
    private static String text(GSYVVideoActivity a) { StringBuilder text = new StringBuilder(); for (androidx.media3.common.text.Cue cue : player(a).getSubtitleCues().cues) if (cue.text != null) text.append(cue.text); return text.toString(); }
    private static AlertDialog dialog(GSYVVideoActivity a) {
        try { java.lang.reflect.Field controls = GSYVVideoActivity.class.getDeclaredField("playbackControls"); controls.setAccessible(true);
            java.lang.reflect.Field dialog = GsyPlaybackControls.class.getDeclaredField("settingsDialog"); dialog.setAccessible(true);
            return (AlertDialog) dialog.get(controls.get(a));
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private static void choose(GSYVVideoActivity a, int index) { ListView list = dialog(a).getListView(); list.performItemClick(list.getChildAt(index), index, list.getAdapter().getItemId(index)); }
    private void await(ActivityScenario<GSYVVideoActivity> page, Check check) {
        long end = SystemClock.elapsedRealtime() + 20000; AtomicBoolean done = new AtomicBoolean();
        do { page.onActivity(a -> { try { done.set(check.ready(a)); } catch (NullPointerException ignored) { } });
            if (done.get()) return; SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < end);
        page.onActivity(a -> {
            StringBuilder detail = new StringBuilder("Expected native subtitle state; cues=").append(text(a));
            for (VideoTrackChoices choice : VideoTrackChoices.supported(kernel().getCurrentTracks(), androidx.media3.common.C.TRACK_TYPE_TEXT)) {
                detail.append("; ").append(choice.group.getTrackFormat(choice.index)).append(" selected=").append(choice.group.isTrackSelected(choice.index));
            }
            fail(detail.toString());
        });
    }
    private interface Check { boolean ready(GSYVVideoActivity a); }
    private void capture(String name) throws Exception {
        SystemClock.sleep(350); File dir = new File(context.getExternalFilesDir(null), "verification-subtitles"); dir.mkdirs();
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) { image.compress(Bitmap.CompressFormat.PNG, 100, out); }
        image.recycle();
    }
}
