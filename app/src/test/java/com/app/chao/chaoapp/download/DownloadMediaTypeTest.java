package com.app.chao.chaoapp.download;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class DownloadMediaTypeTest {
    @Test public void signedHlsAndDashUrlsKeepTheirFormats() {
        assertEquals(DownloadMediaType.HLS, DownloadMediaType.detect(
                "https://example.com/film.m3u8?token=one.mp4", null, new byte[0]));
        assertEquals(DownloadMediaType.DASH, DownloadMediaType.detect(
                "https://example.com/manifest.mpd?signature=abc", null, new byte[0]));
    }

    @Test public void extensionlessEndpointsUseHeadersAndContent() {
        assertEquals(DownloadMediaType.HLS, DownloadMediaType.detect("https://example.com/play",
                "application/octet-stream", "#EXTM3U\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals(DownloadMediaType.DASH, DownloadMediaType.detect("https://example.com/play",
                "application/dash+xml; charset=utf-8", new byte[0]));
        assertEquals("video/webm", DownloadMediaType.detect("https://example.com/play",
                "video/webm", new byte[0]));
    }

    @Test public void invalidPageAndUnsupportedSchemeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DownloadMediaType.detect(
                "https://example.com/film.mp4", "text/html", "<html>sign in</html>".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> DownloadMediaType.detect(
                "https://example.com/film.mp4", "application/json", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> DownloadMediaType.detect(
                "content://media/1", null, new byte[0]));
    }

    @Test public void taskIdentityIsStableWithoutExposingTheSourceUrl() {
        String url = "https://example.com/film.mp4?token=secret";
        assertEquals(DownloadMediaType.id(url), DownloadMediaType.id(url));
        assertEquals(64, DownloadMediaType.id(url).length());
        assertNotEquals(DownloadMediaType.id(url), DownloadMediaType.id(url + "2"));
    }
}
