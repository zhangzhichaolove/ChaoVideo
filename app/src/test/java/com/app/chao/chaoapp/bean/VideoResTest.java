package com.app.chao.chaoapp.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class VideoResTest {
    @Test
    public void replacesExistingEpisodeSuffix() {
        assertEquals("/resources/video/Doctor异乡人_12.mp4",
                VideoRes.episodePath("/resources/video/Doctor异乡人_1.mp4", 12));
    }

    @Test
    public void addsEpisodeSuffixWhenSourceHasNone() {
        assertEquals("/resources/video/movie_3.mp4",
                VideoRes.episodePath("/resources/video/movie.mp4", 3));
    }

    @Test
    public void preservesInvalidEpisodeInput() {
        assertEquals("/resources/video/movie.mp4",
                VideoRes.episodePath("/resources/video/movie.mp4", 0));
        assertNull(VideoRes.episodePath(null, 2));
    }
}
