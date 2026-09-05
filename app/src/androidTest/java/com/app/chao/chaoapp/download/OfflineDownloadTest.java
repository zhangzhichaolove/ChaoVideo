package com.app.chao.chaoapp.download;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.scheduler.Requirements;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.app.chao.chaoapp.ui.activity.DownloadsActivity;
import com.app.chao.chaoapp.ui.activity.OfflineVideoActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Uses generated local media, real DownloadService, native decoders and a server shut down before playback. */
@RunWith(AndroidJUnit4.class)
@androidx.annotation.OptIn(markerClass = UnstableApi.class)
public class OfflineDownloadTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void mp4HlsDashDownloadPauseRetryAndPlayWithServerOffline() throws Exception {
        List<DownloadRequest> requests = new ArrayList<>();
        try (ActivityScenario<DownloadsActivity> center = ActivityScenario.launch(DownloadsActivity.class);
             FixtureServer server = new FixtureServer()) {
            AtomicReference<VideoDownloads> storeReference = new AtomicReference<>();
            center.onActivity(activity -> {
                VideoDownloads store = VideoDownloads.get(activity);
                storeReference.set(store);
                store.manager().setRequirements(new Requirements(0));
                store.manager().setMinRetryCount(0);
                DownloadService.sendRemoveAllDownloads(context, VideoDownloadService.class, true);
            });
            VideoDownloads store = storeReference.get();
            String[] paths = {"sample.mp4", "hls/index.m3u8", "dash/manifest.mpd"};
            for (String path : paths) {
                CountDownLatch prepared = new CountDownLatch(1);
                AtomicReference<DownloadRequest> request = new AtomicReference<>();
                AtomicReference<String> preparationError = new AtomicReference<>();
                center.onActivity(activity -> store.prepare(server.url(path), path, new VideoDownloads.RequestCallback() {
                    @Override public void onReady(DownloadRequest ready) { request.set(ready); prepared.countDown(); }
                    @Override public void onError(String error) { preparationError.set(error); prepared.countDown(); }
                }));
                assertTrue("Preparation timed out: " + path, prepared.await(40, TimeUnit.SECONDS));
                assertNull(preparationError.get(), preparationError.get());
                assertNotNull(request.get());
                requests.add(request.get());
                // Per-task pause is durable and does not start a partial download.
                center.onActivity(activity -> DownloadService.sendAddDownload(context,
                        VideoDownloadService.class, request.get(), VideoDownloads.USER_PAUSED, true));
                awaitState(store.manager(), request.get().id, Download.STATE_STOPPED);
                // Exercise a real server error and then retry the same task identity.
                server.failAll = true;
                center.onActivity(activity -> DownloadService.sendSetStopReason(context,
                        VideoDownloadService.class, request.get().id, Download.STOP_REASON_NONE, true));
                awaitState(store.manager(), request.get().id, Download.STATE_FAILED);
                server.failAll = false;
                center.onActivity(activity -> DownloadService.sendAddDownload(context,
                        VideoDownloadService.class, request.get(), true));
                Download completed = awaitState(store.manager(), request.get().id, Download.STATE_COMPLETED);
                assertTrue("No media bytes: " + path, completed.getBytesDownloaded() > 8192);
            }
            center.onActivity(activity -> assertEquals(activity.getString(com.app.chao.chaoapp.R.string.downloads_title),
                    activity.getSupportActionBar().getTitle().toString()));
            SystemClock.sleep(1200); // Allow the download center to refresh its completed rows.
            capture("downloads-complete");
            server.close(); // No fixture URL remains reachable for any of the playback checks below.
            for (DownloadRequest request : requests) assertOfflinePlayback(request.id);
            // Delete removes both task and its cached payload; completed downloads are not permanent orphans.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (DownloadRequest request : requests) {
                    DownloadService.sendRemoveDownload(context, VideoDownloadService.class, request.id, true);
                }
            });
            long deadline = SystemClock.elapsedRealtime() + 15000;
            for (DownloadRequest request : requests) {
                while (store.manager().getDownloadIndex().getDownload(request.id) != null
                        && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50);
                assertNull(store.manager().getDownloadIndex().getDownload(request.id));
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    {
                        store.manager().setRequirements(new Requirements(Requirements.NETWORK | Requirements.DEVICE_STORAGE_NOT_LOW));
                        store.manager().setMinRetryCount(DownloadManager.DEFAULT_MIN_RETRY_COUNT);
                    });
        }
    }

    private Download awaitState(DownloadManager manager, String id, int state) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + 40000;
        Download latest = null;
        while (SystemClock.elapsedRealtime() < deadline) {
            latest = manager.getDownloadIndex().getDownload(id);
            if (latest != null && latest.state == state) return latest;
            // Service commands are asynchronous: the previous FAILED row can still be visible
            // immediately after a retry intent. Only a terminal timeout proves that retry failed.
            SystemClock.sleep(50);
        }
        throw new AssertionError("Expected " + state + ", last state=" + (latest == null ? null : latest.state));
    }

    private void assertOfflinePlayback(String id) throws Exception {
        assertPlayback(new Intent(context, OfflineVideoActivity.class)
                .putExtra(OfflineVideoActivity.EXTRA_DOWNLOAD_ID, id), id.substring(0, 8));
    }

    @Test public void localContentUriDecodesWithoutStoragePermission() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 29);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "ChaoVideo-fixture-" + System.nanoTime() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ChaoVideoTest");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);
        try {
            try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                    .open("offline-fixtures/sample.mp4"); OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                assertNotNull(output);
                byte[] bytes = new byte[8192];
                int read;
                while ((read = input.read(bytes)) != -1) output.write(bytes, 0, read);
            }
            values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0);
            context.getContentResolver().update(uri, values, null, null);
            assertPlayback(new Intent(context, OfflineVideoActivity.class).setData(uri), "local-content-uri");
        } finally { context.getContentResolver().delete(uri, null, null); }
    }

    private void assertPlayback(Intent intent, String screenshotName) throws Exception {
        try (ActivityScenario<OfflineVideoActivity> playback = ActivityScenario.launch(intent)) {
            AtomicReference<String> result = new AtomicReference<>();
            long deadline = SystemClock.elapsedRealtime() + 20000;
            boolean captured = false;
            while (result.get() == null && SystemClock.elapsedRealtime() < deadline) {
                playback.onActivity(activity -> {
                    try {
                        Field field = OfflineVideoActivity.class.getDeclaredField("player");
                        field.setAccessible(true);
                        ExoPlayer player = (ExoPlayer) field.get(activity);
                        if (player == null) return;
                        if (player.getPlayerError() != null) {
                            result.set(player.getPlayerError().getErrorCodeName());
                        } else if (player.getPlaybackState() == Player.STATE_ENDED) {
                            if (player.getVideoDecoderCounters() != null) {
                                player.getVideoDecoderCounters().ensureUpdated();
                            }
                            result.set(player.getDuration() >= 3500 && player.getVideoDecoderCounters() != null
                                    && player.getVideoDecoderCounters().renderedOutputBufferCount > 0 ? "played" : "no decoded video");
                        }
                    } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                });
                if (!captured) {
                    SystemClock.sleep(600);
                    capture("offline-" + screenshotName);
                    captured = true;
                }
                SystemClock.sleep(50);
            }
            assertEquals("Offline playback must decode frames and finish with the server closed", "played", result.get());
        }
    }

    private void capture(String name) throws Exception {
        File directory = new File(context.getExternalFilesDir(null), "verification");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { screenshot.recycle(); }
    }

}
