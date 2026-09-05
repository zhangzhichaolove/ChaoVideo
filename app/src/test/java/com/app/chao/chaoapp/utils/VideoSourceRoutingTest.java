package com.app.chao.chaoapp.utils;

import static org.junit.Assert.*;
import android.content.Intent;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoRecordEntity;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.app.chao.chaoapp.ui.activity.OfflineVideoActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class VideoSourceRoutingTest {
    private org.robolectric.android.controller.ActivityController<android.app.Activity> controller;
    @org.junit.Before public void setup() {
        controller = org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup();
    }
    @org.junit.After public void close() { controller.close(); }
    @Test public void apiIdPrefixDoesNotRouteToPrivateOfflineContent() {
        VideoRes video = new VideoRes();
        video.setId("download:123"); video.setVideo("movie.mp4");
        video.bindApiSource("https://example.com/a/");
        JumpUtil.goGSYYVideoActivity(controller.get(), video);
        Intent intent = Shadows.shadowOf(controller.get()).getNextStartedActivity();
        assertEquals(GSYVVideoActivity.class.getName(), intent.getComponent().getClassName());
    }

    @Test public void explicitlyLocalSourceUsesContentPlayerWithoutChangingApi() {
        VideoRes video = new VideoRes();
        video.setId("local:123"); video.setVideo("content://media/video/123");
        video.bindOfflineSource(false);
        JumpUtil.goGSYYVideoActivity(controller.get(), video);
        Intent intent = Shadows.shadowOf(controller.get()).getNextStartedActivity();
        assertEquals(OfflineVideoActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(video.getVideo(), intent.getDataString());
    }

    @Test public void oldLibraryDownloadCarriesItsEstablishedProgressKey() {
        VideoRecordEntity row = new VideoRecordEntity();
        row.videoId = "download:123"; row.videoUrl = "https://cdn.example/movie.mp4";
        row.videoKey = "id:download:123";
        JumpUtil.goGSYYVideoActivity(controller.get(), row.toVideo());
        Intent intent = Shadows.shadowOf(controller.get()).getNextStartedActivity();
        assertEquals(OfflineVideoActivity.class.getName(), intent.getComponent().getClassName());
        VideoRes legacy = androidx.core.content.IntentCompat.getParcelableExtra(
                intent, OfflineVideoActivity.EXTRA_LEGACY_RECORD, VideoRes.class);
        assertEquals(row.videoKey, VideoRecordEntity.keyOf(legacy));
    }
}
