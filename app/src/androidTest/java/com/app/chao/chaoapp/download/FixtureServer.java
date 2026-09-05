package com.app.chao.chaoapp.download;

import androidx.test.platform.app.InstrumentationRegistry;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Loopback-only range server for synthetic instrumentation assets. */
public final class FixtureServer implements Closeable {
    private final ServerSocket server;
    private final Thread worker;
    public volatile boolean failAll;
    private final String assetRoot;
    public volatile java.util.concurrent.CountDownLatch responseGate;
    public volatile String gatedPath;
    public final java.util.Map<String, String> jsonResponses = new java.util.concurrent.ConcurrentHashMap<>();
    public final java.util.Map<String, String> assetAliases = new java.util.concurrent.ConcurrentHashMap<>();
    public final java.util.Set<String> requested = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    public final java.util.Set<String> requestedTargets = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public FixtureServer() throws Exception { this("offline-fixtures"); }

    public FixtureServer(String assetRoot) throws Exception {
        this.assetRoot = assetRoot;
        server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        worker = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) { serve(socket); }
                catch (Exception ignored) { if (server.isClosed()) return; }
            }
        }, "offline-fixture-http");
        worker.start();
    }

    public String url(String path) { return "http://127.0.0.1:" + server.getLocalPort() + "/" + path; }

    private void serve(Socket socket) throws Exception {
        socket.setSoTimeout(5000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        String requestLine = reader.readLine();
        if (requestLine == null) return;
        requestedTargets.add(requestLine.split(" ")[1]);
        String path = requestLine.split(" ")[1].substring(1).split("\\?", 2)[0];
        requested.add(path);
        String range = null;
        String header;
        while ((header = reader.readLine()) != null && !header.isEmpty()) {
            if (header.toLowerCase(Locale.ROOT).startsWith("range:")) range = header.substring(6).trim();
        }
        if (failAll) { reply(socket, 503, "text/plain", new byte[0], null, false); return; }
        java.util.concurrent.CountDownLatch gate = responseGate;
        if (gate != null && (gatedPath == null || gatedPath.equals(path))
                && !gate.await(15, java.util.concurrent.TimeUnit.SECONDS)) return;
        String json = jsonResponses.get(path);
        if (json != null) {
            reply(socket, 200, "application/json", json.getBytes(StandardCharsets.UTF_8), null, false);
            return;
        }
        byte[] bytes;
        String assetPath = assetAliases.get(path);
        if (assetPath == null) assetPath = path;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                .open(assetRoot + "/" + assetPath); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            bytes = output.toByteArray();
        }
        String mime = path.endsWith(".m3u8") ? DownloadMediaType.HLS
                : path.endsWith(".mpd") ? DownloadMediaType.DASH
                : path.endsWith(".ts") ? "video/mp2t"
                : path.endsWith(".mkv") ? "video/x-matroska" : "video/mp4";
        reply(socket, 200, mime, bytes, range, requestLine.startsWith("HEAD "));
    }

    private void reply(Socket socket, int status, String mime, byte[] body, String range, boolean head) throws Exception {
        int start = 0;
        int end = body.length - 1;
        if (range != null && range.startsWith("bytes=")) {
            String[] values = range.substring(6).split("-", -1);
            start = Integer.parseInt(values[0]);
            if (values.length > 1 && !values[1].isEmpty()) end = Math.min(end, Integer.parseInt(values[1]));
            status = start >= body.length ? 416 : 206;
        }
        int length = status == 416 ? 0 : Math.max(0, end - start + 1);
        String headers = "HTTP/1.1 " + status + " Result\r\nContent-Type: " + mime
                + "\r\nContent-Length: " + length + "\r\nConnection: close\r\nAccept-Ranges: bytes\r\n";
        if (status == 206) headers += "Content-Range: bytes " + start + "-" + end + "/" + body.length + "\r\n";
        OutputStream output = socket.getOutputStream();
        output.write((headers + "\r\n").getBytes(StandardCharsets.US_ASCII));
        if (!head && length > 0) output.write(body, start, length);
        output.flush();
    }

    @Override public void close() throws java.io.IOException { server.close(); }
}
