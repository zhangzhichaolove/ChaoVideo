package com.app.chao.chaoapp.playback;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.download.FixtureServer;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoDatabase;
import com.app.chao.chaoapp.data.VideoLibraryRepository;
import com.app.chao.chaoapp.data.VideoRecordEntity;
import com.app.chao.chaoapp.net.ApiAddressManager;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.app.chao.chaoapp.ui.activity.OfflineVideoActivity;
import com.google.android.material.tabs.TabLayout;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;

/** Disposable-emulator regression: real GSY/Media3 kernels, synthetic media and UI dialogs. */
@RunWith(AndroidJUnit4.class)
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class PlaybackSettingsExperienceTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before public void defaults() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 31);
        VideoPlaybackSettings settings = new VideoPlaybackSettings(context);
        settings.setSpeed(1.25f);
        settings.setAspect(VideoPlaybackSettings.FIT);
    }

    @After public void restoreDefaults() {
        VideoPlaybackSettings settings = new VideoPlaybackSettings(context);
        settings.setSpeed(1f);
        settings.setAspect(VideoPlaybackSettings.FIT);
    }

    @Test public void preparationFinishingInBackgroundWaitsForResume() throws Exception {
        try (FixtureServer server = new FixtureServer("playback-fixtures")) {
            server.responseGate = new java.util.concurrent.CountDownLatch(1);
            VideoRes video = new VideoRes();
            video.setId("prepare-fixture-" + System.nanoTime());
            video.setTitle("Deferred preparation fixture");
            video.setVideo(server.url("multi-track.mkv"));
            try (ActivityScenario<GSYVVideoActivity> scenario = ActivityScenario.launch(
                    new Intent(context, GSYVVideoActivity.class).putExtra("videoInfo", video))) {
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PREPAREING);
                // Exercise a normal background transition (the other test exercises visible PiP).
                scenario.onActivity(activity -> activity.setPictureInPictureParams(
                        new android.app.PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()));
                scenario.moveToState(Lifecycle.State.CREATED);
                server.responseGate.countDown();
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PAUSE);
                scenario.onActivity(activity -> assertFalse(kernel().isPlaying()));
                scenario.moveToState(Lifecycle.State.RESUMED);
                await(scenario, activity -> kernel().isPlaying());
                scenario.onActivity(activity -> assertEquals(1.25f, kernel().getSpeed(), 0.001f));
            }
        }
    }

    @Test public void pictureInPictureActionsControlCurrentPlayback() throws Exception {
        try (FixtureServer server = new FixtureServer("playback-fixtures")) {
            VideoRes video = new VideoRes();
            video.setId("pip-fixture-" + System.nanoTime());
            video.setTitle("PiP playback fixture");
            video.setVideo(server.url("multi-track.mkv"));
            try (ActivityScenario<GSYVVideoActivity> scenario = ActivityScenario.launch(
                    new Intent(context, GSYVVideoActivity.class).putExtra("videoInfo", video))) {
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                scenario.onActivity(activity -> ((Toolbar) activity.findViewById(R.id.toolbar)).getMenu()
                        .performIdentifierAction(R.id.action_picture_in_picture, 0));
                await(scenario, Activity::isInPictureInPictureMode);
                scenario.onActivity(activity -> assertTrue("Visible PiP keeps playing", kernel().isPlaying()));
                context.sendBroadcast(new Intent("com.app.chao.chaoapp.action.PIP_PLAY_PAUSE").setPackage(context.getPackageName()));
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PAUSE);
                scenario.onActivity(activity -> assertFalse(kernel().isPlaying()));
                context.sendBroadcast(new Intent("com.app.chao.chaoapp.action.PIP_PLAY_PAUSE").setPackage(context.getPackageName()));
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                SystemClock.sleep(350);
                capture("pip-playback");
            }
        }
    }

    @Test public void onlineSpeedAspectAudioFullscreenAndPausedBackground() throws Exception {
        Uri uri = insertFixture("playback-fixtures/multi-track.mkv", "video/x-matroska");
        try (FixtureServer server = new FixtureServer("playback-fixtures")) {
            VideoRes video = new VideoRes();
            video.setId("playback-fixture-" + System.nanoTime());
            video.setTitle("Playback settings fixture");
            video.setVideo(server.url("multi-track.mkv"));
            Intent intent = new Intent(context, GSYVVideoActivity.class).putExtra("videoInfo", video);
            try (ActivityScenario<GSYVVideoActivity> scenario = ActivityScenario.launch(intent)) {
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                scenario.onActivity(activity -> {
                    assertEquals(1.25f, kernel().getSpeed(), 0.001f);
                    assertEquals(2, VideoTrackChoices.supported(kernel().getCurrentTracks(), C.TRACK_TYPE_AUDIO).size());
                    TabLayout tabs = activity.findViewById(R.id.viewpagertab);
                    assertEquals("Only introduction; no unimplemented comment entry", 1, tabs.getTabCount());
                    Toolbar toolbar = activity.findViewById(R.id.toolbar);
                    activity.onPrepareOptionsMenu(toolbar.getMenu());
                    assertTrue(toolbar.getMenu().findItem(R.id.action_audio_track).isVisible());
                    assertTrue(toolbar.getMenu().performIdentifierAction(R.id.action_speed, 0));
                    selectDialog(controls(activity), 5); // 1.5x
                    assertEquals(1.5f, kernel().getSpeed(), 0.001f);
                    toolbar.getMenu().performIdentifierAction(R.id.action_video_aspect, 0);
                    selectDialog(controls(activity), VideoPlaybackSettings.CROP);
                    assertEquals(GSYVideoType.SCREEN_TYPE_FULL, GSYVideoType.getShowType());
                    toolbar.getMenu().performIdentifierAction(R.id.action_audio_track, 0);
                    selectDialog(controls(activity), 1);
                });
                await(scenario, activity -> selectedAudio(kernel().getCurrentTracks(), "ja"));
                scenario.onActivity(activity -> {
                    online(activity).onVideoPause();
                    online(activity).getFullscreenButton().performClick();
                });
                await(scenario, activity -> online(activity).isIfCurrentIsFullscreen()
                        && online(activity).findViewWithTag("playback_settings") != null);
                scenario.onActivity(activity -> {
                    assertEquals(GSYVideoView.CURRENT_STATE_PAUSE, online(activity).getCurrentState());
                    online(activity).findViewWithTag("playback_settings").performClick();
                    selectDialog(controls(activity), 0); // Speed submenu
                    selectDialog(controls(activity), 7); // 2x
                    assertEquals(2f, kernel().getSpeed(), 0.001f);
                    assertFalse("Changing settings must not resume paused playback", kernel().isPlaying());
                });
                capture("online-fullscreen-settings");
                scenario.onActivity(activity -> assertTrue(GSYVideoManager.backFromWindowFull(activity)));
                await(scenario, activity -> !online(activity).isIfCurrentIsFullscreen());
                scenario.moveToState(Lifecycle.State.CREATED);
                scenario.moveToState(Lifecycle.State.RESUMED);
                scenario.onActivity(activity -> {
                    assertFalse("Explicit pause must survive background/resume", kernel().isPlaying());
                    assertEquals(2f, kernel().getSpeed(), 0.001f);
                    assertEquals(GSYVideoType.SCREEN_TYPE_FULL, GSYVideoType.getShowType());
                });
            }
            // Shared preference handoff to the separate offline engine, including Activity recreation.
            try (ActivityScenario<OfflineVideoActivity> offline = ActivityScenario.launch(
                    new Intent(context, OfflineVideoActivity.class).setData(uri))) {
                await(offline, activity -> offline(activity) != null && offline(activity).getPlaybackState() == Player.STATE_READY);
                offline.onActivity(activity -> {
                    assertEquals(2f, offline(activity).getPlaybackParameters().speed, 0.001f);
                    assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                            ((PlayerView) activity.findViewById(R.id.offline_player)).getResizeMode());
                    offline(activity).pause();
                });
                offline.recreate();
                await(offline, activity -> offline(activity) != null && offline(activity).getPlaybackState() == Player.STATE_READY);
                offline.onActivity(activity -> assertFalse(offline(activity).getPlayWhenReady()));
            }
        } finally { context.getContentResolver().delete(uri, null, null); }
    }

    @Test public void offlineEmbeddedSubtitlesAudioAndBuiltInSpeedAreReal() throws Exception {
        Uri uri = insertFixture("playback-fixtures/multi-track.mkv", "video/x-matroska");
        try (ActivityScenario<OfflineVideoActivity> scenario = ActivityScenario.launch(
                new Intent(context, OfflineVideoActivity.class).setData(uri))) {
            await(scenario, activity -> offline(activity) != null && offline(activity).getPlaybackState() == Player.STATE_READY);
            scenario.onActivity(activity -> {
                ExoPlayer player = offline(activity);
                assertEquals(2, VideoTrackChoices.supported(player.getCurrentTracks(), C.TRACK_TYPE_AUDIO).size());
                assertEquals(1, VideoTrackChoices.supported(player.getCurrentTracks(), C.TRACK_TYPE_TEXT).size());
                PlayerView view = activity.findViewById(R.id.offline_player);
                assertEquals(View.VISIBLE, view.findViewById(androidx.media3.ui.R.id.exo_subtitle).getVisibility());
                assertEquals(1.25f, player.getPlaybackParameters().speed, 0.001f);
                player.setPlaybackSpeed(1.75f); // Same event path as Media3's built-in speed menu.
                player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                        .setPreferredAudioLanguage("ja").setPreferredTextLanguage("en").build());
                player.seekTo(2000);
            });
            await(scenario, activity -> selectedAudio(offline(activity).getCurrentTracks(), "ja")
                    && !offline(activity).getCurrentCues().cues.isEmpty());
            scenario.onActivity(activity -> {
                offline(activity).pause();
                ((PlayerView) activity.findViewById(R.id.offline_player)).hideController();
            });
            SystemClock.sleep(250); // Let the selected cue draw, not just arrive at the decoder.
            capture("offline-subtitle");
            scenario.onActivity(activity -> {
                assertEquals("ChaoVideo subtitle fixture", offline(activity).getCurrentCues().cues.get(0).text.toString());
                assertEquals(1.75f, new VideoPlaybackSettings(activity).speed(), 0.001f);
                offline(activity).setTrackSelectionParameters(offline(activity).getTrackSelectionParameters().buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build());
                Toolbar toolbar = activity.findViewById(R.id.offline_toolbar);
                toolbar.getMenu().performIdentifierAction(R.id.action_video_aspect, 0);
                selectDialog(activity, VideoPlaybackSettings.STRETCH);
            });
            await(scenario, activity -> offline(activity).getCurrentCues().cues.isEmpty());
            scenario.onActivity(activity -> {
                assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        ((PlayerView) activity.findViewById(R.id.offline_player)).getResizeMode());
                offline(activity).pause();
            });
            scenario.recreate();
            await(scenario, activity -> offline(activity) != null && offline(activity).getPlaybackState() == Player.STATE_READY);
            scenario.onActivity(activity -> {
                assertEquals(1.75f, offline(activity).getPlaybackParameters().speed, 0.001f);
                assertFalse(offline(activity).getPlayWhenReady());
                assertTrue(offline(activity).getTrackSelectionParameters().disabledTrackTypes.contains(C.TRACK_TYPE_TEXT));
                assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        ((PlayerView) activity.findViewById(R.id.offline_player)).getResizeMode());
            });
        } finally { context.getContentResolver().delete(uri, null, null); }
    }

    @Test public void subtitleControlIsHiddenForMediaWithoutSubtitles() throws Exception {
        Uri uri = insertFixture("offline-fixtures/sample.mp4", "video/mp4");
        try (ActivityScenario<OfflineVideoActivity> scenario = ActivityScenario.launch(
                new Intent(context, OfflineVideoActivity.class).setData(uri))) {
            await(scenario, activity -> offline(activity) != null && offline(activity).getPlaybackState() == Player.STATE_READY);
            scenario.onActivity(activity -> {
                assertTrue(VideoTrackChoices.supported(offline(activity).getCurrentTracks(), C.TRACK_TYPE_TEXT).isEmpty());
                PlayerView view = activity.findViewById(R.id.offline_player);
                assertEquals(View.GONE, view.findViewById(androidx.media3.ui.R.id.exo_subtitle).getVisibility());
            });
        } finally { context.getContentResolver().delete(uri, null, null); }
    }

    @Test public void explicitEpisodesSwitchInFullscreenPersistAndAdvanceWithoutGuessingUrls() throws Exception {
        String previous = ApiAddressManager.getBaseUrl();
        try (FixtureServer server = new FixtureServer("playback-fixtures")) {
            String id = "explicit-episodes-" + System.nanoTime();
            server.jsonResponses.put("series/video/getVideoList", "{\"success\":true,\"result\":{\"records\":[{"
                    + "\"id\":\"" + id + "\",\"title\":\"Explicit episode fixture\",\"episodes\":99,"
                    + "\"video\":\"must-not-guess_1.mkv\",\"episodeUrls\":[\"opening.mkv?token=a.b_9\","
                    + "\"different/finale.mkv?token=c%2Fd\",\"bonus.mkv\"]}]}}");
            server.assetAliases.put("series/opening.mkv", "multi-track.mkv");
            server.assetAliases.put("series/different/finale.mkv", "multi-track.mkv");
            server.assetAliases.put("series/bonus.mkv", "multi-track.mkv");
            assertTrue(ApiAddressManager.saveBaseUrl(server.url("series/")));
            VideoRes video = RetrofitHelper.getVideoApi().getVideoList(1).blockingFirst().getResult().getRecords().get(0);
            assertEquals(3, video.getEpisodes());
            VideoLibraryRepository library = VideoLibraryRepository.get(context);
            library.toggleFavorite(video, null);
            try (ActivityScenario<GSYVVideoActivity> scenario = ActivityScenario.launch(
                    new Intent(context, GSYVVideoActivity.class).putExtra("videoInfo", video))) {
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                scenario.onActivity(activity -> online(activity).getFullscreenButton().performClick());
                await(scenario, activity -> online(activity).isIfCurrentIsFullscreen()
                        && online(activity).findViewWithTag("episode_picker") != null);
                // Repeated full-screen switches must configure the actual fullscreen player.
                for (int episode : new int[]{3, 1}) {
                    scenario.onActivity(activity -> online(activity).findViewWithTag("episode_picker").performClick());
                    clickText("第" + episode + "集");
                    await(scenario, activity -> ((Integer) field(activity, "currentEpisode")) == episode
                            && online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                    scenario.onActivity(activity -> {
                        assertTrue(online(activity).isIfCurrentIsFullscreen());
                        assertEquals(video.getEpisodeVideo(episode), field(activity, "currentVideoUrl"));
                        assertEquals(1.25f, kernel().getSpeed(), 0.001f);
                    });
                }
                scenario.onActivity(activity -> kernel().seekTo(12000));
                await(scenario, activity -> kernel().getCurrentPosition() >= 11500);
                scenario.onActivity(activity -> online(activity).findViewWithTag("episode_picker").performClick());
                clickText("第2集");
                await(scenario, activity -> ((Integer) field(activity, "currentEpisode")) == 2
                        && online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                scenario.onActivity(activity -> {
                    assertTrue("A new episode must not inherit previous position", kernel().getCurrentPosition() < 5000);
                    online(activity).onVideoPause();
                    kernel().seekTo(23000);
                });
                await(scenario, activity -> kernel().getCurrentPosition() >= 22500);
                capture("explicit-episode-fullscreen");
                scenario.onActivity(activity -> assertTrue(GSYVideoManager.backFromWindowFull(activity)));
                await(scenario, activity -> !online(activity).isIfCurrentIsFullscreen());
                scenario.onActivity(activity -> assertFalse(kernel().isPlaying()));
            }
            java.util.concurrent.CountDownLatch stored = new java.util.concurrent.CountDownLatch(1);
            library.loadLastEpisode(video, value -> stored.countDown());
            assertTrue(stored.await(10, java.util.concurrent.TimeUnit.SECONDS));
            VideoRecordEntity record = VideoDatabase.get(context).libraryDao().find(VideoRecordEntity.keyOf(video));
            assertTrue(record.favorite);
            assertEquals(2, record.lastEpisode);
            assertTrue(record.positionMs >= 22500);
            assertArrayEquals(video.getEpisodeUrls(), record.toVideo().getEpisodeUrls());
            assertTrue(VideoDatabase.get(context).libraryDao().progress(record.videoKey, 1).positionMs >= 11500);
            assertTrue(ApiAddressManager.saveBaseUrl(server.url("other/")));
            try (ActivityScenario<GSYVVideoActivity> scenario = ActivityScenario.launch(
                    new Intent(context, GSYVVideoActivity.class).putExtra("videoInfo", record.toVideo()))) {
                await(scenario, activity -> field(activity, "resumeDialog") != null);
                scenario.onActivity(activity -> ((AlertDialog) field(activity, "resumeDialog"))
                        .getButton(AlertDialog.BUTTON_POSITIVE).performClick());
                await(scenario, activity -> online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                scenario.onActivity(activity -> {
                    assertEquals(2, field(activity, "currentEpisode"));
                    assertTrue(kernel().getCurrentPosition() >= 22500);
                    assertEquals(video.getEpisodeVideo(2), field(activity, "currentVideoUrl"));
                    kernel().seekTo(29000);
                });
                // Natural decoder completion, not a manually invoked callback, advances to URL 3.
                await(scenario, activity -> ((Integer) field(activity, "currentEpisode")) == 3
                        && online(activity).getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                scenario.onActivity(activity -> assertEquals(video.getEpisodeVideo(3), field(activity, "currentVideoUrl")));
                capture("explicit-episode-auto-next");
            }
            assertTrue(server.requestedTargets.contains("/series/opening.mkv?token=a.b_9"));
            assertTrue(server.requestedTargets.contains("/series/different/finale.mkv?token=c%2Fd"));
            assertTrue(server.requested.contains("series/bonus.mkv"));
            for (String path : server.requested) {
                assertFalse("Never guess a media path", path.contains("must-not-guess"));
                assertFalse("Never switch an old series to the current API", path.startsWith("other/"));
            }
        } finally { ApiAddressManager.saveBaseUrl(previous); }
    }

    private void clickText(String text) {
        long end = SystemClock.elapsedRealtime() + 10000;
        do {
            android.view.accessibility.AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().getRootInActiveWindow();
            if (root != null) for (android.view.accessibility.AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
                if (!text.contentEquals(node.getText() == null ? "" : node.getText())) continue;
                while (!node.isClickable() && node.getParent() != null) node = node.getParent();
                assertTrue(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));
                return;
            }
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < end);
        fail("Missing UI text: " + text);
    }

    private boolean selectedAudio(Tracks tracks, String language) {
        for (VideoTrackChoices choice : VideoTrackChoices.supported(tracks, C.TRACK_TYPE_AUDIO)) {
            if (choice.group.isTrackSelected(choice.index)
                    && language.equals(choice.group.getTrackFormat(choice.index).language)) return true;
        }
        return false;
    }

    private NormalGSYVideoPlayer online(GSYVVideoActivity activity) {
        return (NormalGSYVideoPlayer) ((NormalGSYVideoPlayer) activity.findViewById(R.id.detail_player)).getCurrentPlayer();
    }
    private IjkExo2MediaPlayer kernel() {
        return (IjkExo2MediaPlayer) GSYVideoManager.instance().getCurPlayerManager().getMediaPlayer();
    }
    private ExoPlayer offline(OfflineVideoActivity activity) { return (ExoPlayer) field(activity, "player"); }
    private Object controls(GSYVVideoActivity activity) { return field(activity, "playbackControls"); }
    private Object field(Object object, String name) {
        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private void selectDialog(Object owner, int index) {
        AlertDialog dialog = (AlertDialog) field(owner, "settingsDialog");
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        ListView list = dialog.getListView();
        list.performItemClick(list.getChildAt(index - list.getFirstVisiblePosition()), index, list.getAdapter().getItemId(index));
    }
    private interface Condition<A extends Activity> { boolean ready(A activity); }
    private <A extends Activity> void await(ActivityScenario<A> scenario, Condition<A> condition) {
        long end = SystemClock.elapsedRealtime() + 15_000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> ready.set(condition.ready(activity)));
            if (ready.get()) return;
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < end);
        fail("Player condition timed out");
    }
    private Uri insertFixture(String asset, String mime) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "ChaoVideo-settings-" + System.nanoTime()
                + (mime.equals("video/mp4") ? ".mp4" : ".mkv"));
        values.put(MediaStore.Video.Media.MIME_TYPE, mime);
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ChaoVideoTest");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);
        try {
            try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(asset);
                 OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                assertNotNull(output);
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0);
            context.getContentResolver().update(uri, values, null, null);
            return uri;
        } catch (Exception error) {
            context.getContentResolver().delete(uri, null, null);
            throw error;
        }
    }
    private void capture(String name) throws Exception {
        File directory = new File(context.getExternalFilesDir(null), "verification-playback");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
