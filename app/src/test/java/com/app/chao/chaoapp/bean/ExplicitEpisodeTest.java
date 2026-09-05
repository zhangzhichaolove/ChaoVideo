package com.app.chao.chaoapp.bean;

import static org.junit.Assert.*;
import android.os.Parcel;
import com.app.chao.chaoapp.data.VideoRecordEntity;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class ExplicitEpisodeTest {
    private VideoRes video(String json) {
        VideoRes video = new Gson().fromJson(json, VideoRes.class);
        video.bindApiSource("https://example.com/api/");
        return video;
    }

    @Test public void explicitUrlsOverrideCountAndFilenameWithoutChangingSignedQuery() {
        VideoRes video = video("{\"id\":\"series\",\"video\":\"wrong_1.mp4\",\"episodes\":99,"
                + "\"episodeUrls\":[\"media/first.m3u8?token=a.b_9&x=%2F\",\"https://cdn.example/different.mp4#part\"]}");
        video.validateEpisodeUrls();
        assertEquals(2, video.getEpisodes());
        assertEquals("https://example.com/api/media/first.m3u8?token=a.b_9&x=%2F", video.getEpisodeVideo(1));
        assertEquals("https://cdn.example/different.mp4#part", video.getEpisodeVideo(2));
        assertNull(video.getEpisodeVideo(0));
        assertNull(video.getEpisodeVideo(3));
        video.bindApiSource("https://other.example/");
        assertEquals("https://example.com/api/media/first.m3u8?token=a.b_9&x=%2F", video.getEpisodeVideo(1));
    }

    @Test public void absentOrNullUsesLegacyButEmptyArrayDoesNotInventEpisodes() {
        for (String optional : new String[]{"", ",\"episodeUrls\":null"}) {
            VideoRes video = video("{\"video\":\"show_1.mp4\",\"episodes\":3" + optional + "}");
            assertEquals(3, video.getEpisodes());
            assertEquals("https://example.com/api/show_2.mp4", video.getEpisodeVideo(2));
        }
        VideoRes empty = video("{\"video\":\"movie.mp4\",\"episodes\":99,\"episodeUrls\":[]}");
        assertEquals(0, empty.getEpisodes());
        assertNull(empty.getEpisodeVideo(1));
        assertEquals("https://example.com/api/movie.mp4", empty.getVideo());
    }

    @Test public void listOnlySeriesHasAPlayableUrlAndUrlIdentityFallback() {
        VideoRes video = video("{\"episodeUrls\":[\"first.mkv\"]}");
        assertEquals("https://example.com/api/first.mkv", video.getVideo());
        assertTrue(VideoRecordEntity.keyOf(video).endsWith(":url:https://example.com/api/first.mkv"));
    }

    @Test public void malformedListsFailInsteadOfFallingBackToTheFilename() {
        for (String invalid : new String[]{"[\"\"]", "[\" \" ]", "[\"file:///private/media.mp4\"]", "[\"javascript:play()\"]"}) {
            VideoRes video = video("{\"video\":\"valid.mp4\",\"episodes\":2,\"episodeUrls\":" + invalid + "}");
            assertThrows(IllegalArgumentException.class, video::validateEpisodeUrls);
        }
    }

    @Test public void nonStringEpisodeEntriesAreNotCoercedIntoRelativePaths() {
        for (String invalid : new String[]{"[null]", "[42]", "[true]", "[{}]", "{}", "\"url\""}) {
            assertThrows(com.google.gson.JsonParseException.class,
                    () -> video("{\"episodeUrls\":" + invalid + "}"));
        }
    }

    @Test public void arraysAreDefensiveAndParcelablePreservesSourceAndExplicitOrder() {
        VideoRes video = video("{\"id\":\"series\",\"episodeUrls\":[\"z.mkv\",\"a.mkv\"]}");
        String[] copy = video.getEpisodeUrls(); copy[0] = "changed.mkv";
        assertEquals("https://example.com/api/z.mkv", video.getEpisodeVideo(1));
        video.setEpisodeUrls(copy); copy[0] = "again.mkv";
        Parcel parcel = Parcel.obtain();
        try {
            video.writeToParcel(parcel, 0); parcel.setDataPosition(0);
            VideoRes restored = VideoRes.CREATOR.createFromParcel(parcel);
            assertEquals(2, restored.getEpisodes());
            assertEquals("https://example.com/api/changed.mkv", restored.getEpisodeVideo(1));
            assertEquals("https://example.com/api/a.mkv", restored.getEpisodeVideo(2));
            assertEquals(VideoRecordEntity.keyOf(video), VideoRecordEntity.keyOf(restored));
        } finally { parcel.recycle(); }
    }

    @Test public void legacyInferenceOnlyChangesPathNotQueryOrFragment() {
        assertEquals("/series_3.mp4?token=x_99.end#file_8.ext",
                VideoRes.episodePath("/series_1.mp4?token=x_99.end#file_8.ext", 3));
        assertEquals("/play_2?token=x_99.end", VideoRes.episodePath("/play?token=x_99.end", 2));
        assertEquals("/movie_2.mp4#section?x=1", VideoRes.episodePath("/movie.mp4#section?x=1", 2));
    }
}
