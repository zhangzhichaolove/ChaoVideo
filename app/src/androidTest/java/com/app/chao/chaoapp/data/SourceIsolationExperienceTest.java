package com.app.chao.chaoapp.data;

import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.VideoListAdapter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.download.FixtureServer;
import com.app.chao.chaoapp.net.ApiAddressManager;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.app.chao.chaoapp.ui.activity.VideoLibraryActivity;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SourceIsolationExperienceTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void lateApiAResponseFavoritesAndResumeStayOnSourceAAfterSwitchToB() throws Exception {
        String previous = ApiAddressManager.getBaseUrl();
        Disposable request = null;
        try (FixtureServer server = new FixtureServer("playback-fixtures")) {
            String id = "shared-id-" + System.nanoTime();
            server.jsonResponses.put("a/video/getVideoList", json(id, "Source A fixture"));
            server.jsonResponses.put("b/video/getVideoList", json(id, "Source B fixture"));
            server.assetAliases.put("a/movie.mkv", "multi-track.mkv");
            server.assetAliases.put("b/movie.mkv", "multi-track.mkv");
            server.gatedPath = "a/video/getVideoList";
            server.responseGate = new CountDownLatch(1);
            assertTrue(ApiAddressManager.saveBaseUrl(server.url("a/")));
            CountDownLatch aReady = new CountDownLatch(1);
            AtomicReference<VideoRes> a = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            request = RetrofitHelper.getVideoApi().getVideoList(1).subscribeOn(Schedulers.io())
                    .subscribe(response -> { a.set(response.getResult().getRecords().get(0)); aReady.countDown(); },
                            error -> { failure.set(error); aReady.countDown(); });
            long end = SystemClock.elapsedRealtime() + 10000;
            while (!server.requested.contains("a/video/getVideoList") && SystemClock.elapsedRealtime() < end) SystemClock.sleep(25);
            assertTrue("A must already be in flight", server.requested.contains("a/video/getVideoList"));
            assertTrue(ApiAddressManager.saveBaseUrl(server.url("b/")));
            server.responseGate.countDown();
            assertTrue(aReady.await(10, TimeUnit.SECONDS));
            assertNull(failure.get());
            VideoRes b = RetrofitHelper.getVideoApi().getVideoList(1).blockingFirst().getResult().getRecords().get(0);
            assertEquals(server.url("a/movie.mkv"), a.get().getVideo());
            assertEquals(server.url("b/movie.mkv"), b.getVideo());
            assertNotEquals(VideoRecordEntity.keyOf(a.get()), VideoRecordEntity.keyOf(b));

            VideoLibraryRepository library = VideoLibraryRepository.get(context);
            library.toggleFavorite(a.get(), null);
            library.toggleFavorite(b, null);
            library.updateProgress(a.get(), 0, 12000, 30000);
            library.updateProgress(b, 0, 23000, 30000);
            AtomicReference<List<VideoRes>> saved = new AtomicReference<>();
            CountDownLatch stored = new CountDownLatch(1);
            library.loadFavorites(videos -> { saved.set(videos); stored.countDown(); });
            assertTrue(stored.await(10, TimeUnit.SECONDS));
            VideoRes storedA = null;
            int matches = 0;
            for (VideoRes video : saved.get()) if (id.equals(video.getId())) {
                matches++;
                if (video.getSourceId().equals(a.get().getSourceId())) storedA = video;
            }
            assertEquals(2, matches);
            assertNotNull(storedA);
            assertEquals(12000, storedA.getLocalProgressMs());
            assertEquals(server.url("a/movie.mkv"), storedA.getVideo());

            try (ActivityScenario<VideoLibraryActivity> page = ActivityScenario.launch(VideoLibraryActivity.favoritesIntent(context))) {
                await(page, activity -> {
                    VideoListAdapter adapter = (VideoListAdapter) ((RecyclerView) activity.findViewById(R.id.library_list)).getAdapter();
                    int count = 0;
                    for (VideoRes video : adapter.getCurrentList()) if (id.equals(video.getId())) count++;
                    return count == 2;
                });
                SystemClock.sleep(250);
                page.onActivity(activity -> {
                    RecyclerView list = activity.findViewById(R.id.library_list);
                    VideoListAdapter adapter = (VideoListAdapter) list.getAdapter();
                    int visible = 0;
                    for (int i = 0; i < adapter.getItemCount(); i++) {
                        if (!id.equals(adapter.getItem(i).getId())) continue;
                        RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(i);
                        assertNotNull(holder);
                        TextView label = holder.itemView.findViewById(R.id.tv_source);
                        android.text.Layout layout = label.getLayout();
                        assertNotNull(layout);
                        int last = layout.getLineCount() - 1;
                        assertEquals("Source port and path must be visible", 0, layout.getEllipsisCount(last));
                        assertEquals(label.length(), layout.getLineEnd(last));
                        assertTrue("All source lines must fit inside the measured height",
                                layout.getLineBottom(last) <= label.getHeight()
                                        - label.getCompoundPaddingTop() - label.getCompoundPaddingBottom());
                        visible++;
                    }
                    assertEquals(2, visible);
                });
                capture("sources-in-favorites");
            }
            try (ActivityScenario<GSYVVideoActivity> player = ActivityScenario.launch(new Intent(context, GSYVVideoActivity.class)
                    .putExtra("videoInfo", storedA))) {
                await(player, activity -> field(activity, "resumeDialog") != null);
                player.onActivity(activity -> ((AlertDialog) field(activity, "resumeDialog"))
                        .getButton(AlertDialog.BUTTON_POSITIVE).performClick());
                await(player, activity -> ((NormalGSYVideoPlayer) activity.findViewById(R.id.detail_player)).getCurrentPlayer()
                        .getCurrentState() == GSYVideoView.CURRENT_STATE_PLAYING);
                player.onActivity(activity -> {
                    assertTrue(GSYVideoManager.instance().getCurrentPosition() >= 11000);
                    assertTrue(GSYVideoManager.instance().getCurrentPosition() < 21000);
                });
                assertTrue(server.requested.contains("a/movie.mkv"));
                assertFalse("Saved source A must not resolve on the currently selected B", server.requested.contains("b/movie.mkv"));
                capture("source-a-resume-with-b-selected");
            }
            AtomicReference<VideoProgressEntity> bProgress = new AtomicReference<>();
            CountDownLatch loaded = new CountDownLatch(1);
            library.loadProgress(b, 0, value -> { bProgress.set(value); loaded.countDown(); });
            assertTrue(loaded.await(10, TimeUnit.SECONDS));
            assertEquals(23000, bProgress.get().positionMs);
            writeEvidence("source_a_response_delivered_after_switch_to_b=true\nsame_id_favorite_count=2\n"
                    + "source_a_resumed_position_ms>=11000\nsource_b_unchanged_position_ms=23000\nsource_b_media_requested=false\n");
        } finally {
            if (request != null) request.dispose();
            // The runner can exit before apply() reaches disk; make teardown durable.
            assertTrue(context.getSharedPreferences("api_address", 0).edit().putString("video_api", previous).commit());
        }
    }

    private String json(String id, String title) {
        return "{\"success\":true,\"result\":{\"records\":[{\"id\":\"" + id + "\",\"title\":\"" + title
                + "\",\"video\":\"movie.mkv\",\"episodes\":0}]}}";
    }
    private Object field(Object owner, String name) {
        try {
            java.lang.reflect.Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true); return field.get(owner);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private interface Condition<A extends Activity> { boolean ready(A activity); }
    private <A extends Activity> void await(ActivityScenario<A> scenario, Condition<A> condition) {
        long end = SystemClock.elapsedRealtime() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> ready.set(condition.ready(activity)));
            if (ready.get()) return;
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < end);
        fail("Source scenario timed out");
    }
    private File evidenceDir() {
        File dir = new File(context.getExternalFilesDir(null), "verification-sources");
        assertTrue(dir.isDirectory() || dir.mkdirs()); return dir;
    }
    private void capture(String name) throws Exception {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (FileOutputStream out = new FileOutputStream(new File(evidenceDir(), name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
    private void writeEvidence(String text) throws Exception {
        try (FileOutputStream out = new FileOutputStream(new File(evidenceDir(), "verification.txt"))) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
