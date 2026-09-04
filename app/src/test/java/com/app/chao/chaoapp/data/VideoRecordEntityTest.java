package com.app.chao.chaoapp.data;

import static org.junit.Assert.assertEquals;

import com.app.chao.chaoapp.bean.VideoRes;

import org.junit.Test;

public class VideoRecordEntityTest {
    @Test
    public void keyPrefersServerId() {
        VideoRes video = new VideoRes();
        video.setId("42");
        video.video = "/movie.mp4";
        assertEquals("id:42", VideoRecordEntity.keyOf(video));
    }

    @Test
    public void keyFallsBackToRawUrlWithoutDependingOnCurrentApi() {
        VideoRes video = new VideoRes();
        video.video = "/resources/video/movie.mp4";
        assertEquals("url:/resources/video/movie.mp4", VideoRecordEntity.keyOf(video));
    }
}
