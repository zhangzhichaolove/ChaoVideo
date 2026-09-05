package com.app.chao.chaoapp.data;

import static org.junit.Assert.*;
import android.os.Parcel;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.net.ApiAddressManager;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class VideoSourceTest {
    private VideoRes video(String base, String id, String url) {
        VideoRes video = new VideoRes();
        video.setId(id);
        video.setVideo(url);
        video.setImg("covers/poster.webp");
        video.bindApiSource(base);
        return video;
    }

    @Test public void sameIdsAndSameCdnUrlsAreIsolatedByApiNotMediaHost() {
        VideoRes a = video("https://api.example/a/", "42", "https://cdn.example/movie.mp4");
        VideoRes b = video("https://api.example/b/", "42", "https://cdn.example/movie.mp4");
        assertNotEquals(VideoRecordEntity.keyOf(a), VideoRecordEntity.keyOf(b));
        assertNotEquals(a.getSourceId(), b.getSourceId());
    }

    @Test public void tokenRotationDoesNotChangeAnIdBasedIdentity() {
        assertEquals(VideoRecordEntity.keyOf(video("https://api.example/", "42", "movie.mp4?token=old")),
                VideoRecordEntity.keyOf(video("https://api.example/", "42", "movie.mp4?token=new")));
    }

    @Test public void equivalentBasesNormalizeButPathsPortsAndSchemesRemainDistinct() {
        assertEquals(VideoSource.apiId("https://API.example:443/root"), VideoSource.apiId("https://api.example/root/"));
        assertNotEquals(VideoSource.apiId("https://api.example/a/"), VideoSource.apiId("https://api.example/b/"));
        assertNotEquals(VideoSource.apiId("http://api.example/"), VideoSource.apiId("https://api.example/"));
        assertNotEquals(VideoSource.apiId("http://api.example:80/"), VideoSource.apiId("http://api.example:81/"));
    }

    @Test public void relativeMediaCoverAndEpisodeStayOnTheCapturedSource() {
        VideoRes a = video("https://api.example/a/", "42", "media/show_1.mp4");
        ApiAddressManager.saveBaseUrl("https://different.example/b/");
        assertEquals("https://api.example/a/media/show_1.mp4", a.getVideo());
        assertEquals("https://api.example/a/covers/poster.webp", a.getImg());
        assertEquals("https://api.example/a/media/show_2.mp4", a.getEpisodeVideo(2));
        a.bindApiSource("https://different.example/b/");
        assertEquals("https://api.example/a/", a.getSourceBaseUrl());
    }

    @Test public void missingIdsUseResolvedUrlWithinTheSource() {
        assertEquals(VideoRecordEntity.keyOf(video("https://api.example/a/", null, "movie.mp4")),
                VideoRecordEntity.keyOf(video("https://api.example/a/", " ", "https://api.example/a/movie.mp4")));
        assertNotEquals(VideoRecordEntity.keyOf(video("https://api.example/a/", null, "movie.mp4")),
                VideoRecordEntity.keyOf(video("https://api.example/b/", null, "movie.mp4")));
    }

    @Test public void sourceAndEstablishedLibraryKeySurviveParcel() {
        VideoRes a = video("https://api.example/a/", "42", "movie.mp4");
        VideoRes stored = VideoRecordEntity.from(a).toVideo();
        Parcel parcel = Parcel.obtain();
        try {
            stored.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            VideoRes restored = VideoRes.CREATOR.createFromParcel(parcel);
            assertEquals(a.getSourceId(), restored.getSourceId());
            assertEquals(a.getVideo(), restored.getVideo());
            assertEquals(VideoRecordEntity.keyOf(a), VideoRecordEntity.keyOf(restored));
        } finally { parcel.recycle(); }
    }

    @Test public void apiJsonCannotForgeLocalProvenanceOrExistingLibraryKey() {
        VideoRes forged = new Gson().fromJson("{\"id\":\"download:123\",\"video\":\"movie.mp4\","
                + "\"sourceId\":\"download\",\"sourceBaseUrl\":\"https://attacker.example/\","
                + "\"storedLibraryKey\":\"id:another\"}", VideoRes.class);
        forged.bindApiSource("https://api.example/a/");
        assertEquals(VideoSource.apiId("https://api.example/a/"), forged.getSourceId());
        assertNull(forged.getStoredLibraryKey());
        assertEquals("https://api.example/a/movie.mp4", forged.getVideo());
    }

    @Test public void newLocalAndDownloadRecordsCannotCollideWithApiIds() {
        VideoRes local = new VideoRes();
        local.setId("local:123");
        local.setVideo("content://media/external/video/123");
        local.bindOfflineSource(false);
        VideoRes download = new VideoRes();
        download.setId("local:123");
        download.bindOfflineSource(true);
        assertNotEquals(VideoRecordEntity.keyOf(local), VideoRecordEntity.keyOf(download));
        assertNotEquals(VideoRecordEntity.keyOf(local), VideoRecordEntity.keyOf(video(
                "https://api.example/a/", "local:123", "content://media/external/video/123")));
    }

    @Test public void legacyRawUrlKeyRemainsUsableWithoutBeingReassignedToCurrentApi() {
        VideoRecordEntity row = new VideoRecordEntity();
        row.videoKey = "url:/old/movie.mp4";
        row.videoUrl = "https://old-cdn.example/movie.mp4";
        VideoRes legacy = row.toVideo();
        ApiAddressManager.saveBaseUrl("https://new.example/");
        assertEquals("url:/old/movie.mp4", VideoRecordEntity.keyOf(legacy));
        assertEquals(VideoSource.LEGACY, legacy.getSourceId());
        assertEquals("https://old-cdn.example/movie.mp4", legacy.getVideo());
        assertNull(legacy.getSourceBaseUrl());
    }
}
