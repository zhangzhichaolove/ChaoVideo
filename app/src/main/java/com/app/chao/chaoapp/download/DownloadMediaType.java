package com.app.chao.chaoapp.download;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** Identifies playlists before handing them to the matching Media3 downloader. */
public final class DownloadMediaType {
    public static final String HLS = "application/x-mpegURL";
    public static final String DASH = "application/dash+xml";

    private DownloadMediaType() { }

    public static String detect(String url, String contentType, byte[] prefix) {
        URI uri = URI.create(url);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Only HTTP media can be downloaded");
        }
        String type = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        String text = new String(prefix, StandardCharsets.UTF_8).replace("\uFEFF", "").trim();
        if (text.startsWith("#EXTM3U")) return HLS;
        if (text.contains("<MPD") || text.matches("(?s).*<[\\w-]+:MPD[\\s>].*")) return DASH;
        if (type.contains("mpegurl")) return HLS;
        if (type.equals(DASH)) return DASH;
        if (type.contains("html") || type.contains("json")
                || text.toLowerCase(Locale.ROOT).startsWith("<!doctype html")
                || text.toLowerCase(Locale.ROOT).startsWith("<html")) {
            throw new IllegalArgumentException("The server returned a page, not media");
        }
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (path.endsWith(".m3u8")) return HLS;
        if (path.endsWith(".mpd")) return DASH;
        if (type.startsWith("video/") || type.startsWith("audio/")) return type;
        if (path.endsWith(".webm")) return "video/webm";
        if (path.endsWith(".mkv")) return "video/x-matroska";
        if (path.endsWith(".mp4") || (prefix.length >= 8 && prefix[4] == 'f'
                && prefix[5] == 't' && prefix[6] == 'y' && prefix[7] == 'p')) return "video/mp4";
        return null; // ProgressiveDownloader/ExoPlayer sniff other container formats.
    }

    public static String id(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte b : hash) value.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return value.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
}
