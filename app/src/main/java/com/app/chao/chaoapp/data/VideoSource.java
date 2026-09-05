package com.app.chao.chaoapp.data;

import com.app.chao.chaoapp.net.ApiAddressManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** An API base (including path/port) is a source; a CDN host is not its identity. */
public final class VideoSource {
    public static final String LEGACY = "legacy";
    public static final String LOCAL = "local";
    public static final String DOWNLOAD = "download";

    private VideoSource() { }

    public static String normalize(String baseUrl) {
        String normalized = ApiAddressManager.normalize(baseUrl);
        if (normalized == null) throw new IllegalArgumentException("Invalid video source base URL");
        return normalized;
    }

    public static String apiId(String baseUrl) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(normalize(baseUrl).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException error) { throw new AssertionError(error); }
        StringBuilder id = new StringBuilder("api:");
        for (byte b : digest) id.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
        return id.toString();
    }
}
