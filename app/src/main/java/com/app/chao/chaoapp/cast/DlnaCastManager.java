package com.app.chao.chaoapp.cast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Discovers DLNA MediaRenderer devices and controls their AVTransport service.
 */
public final class DlnaCastManager {
    private static final String SSDP_HOST = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final long SEARCH_DURATION_MS = 4500;
    private static final String CAST_SESSION = "dlna_cast_session";
    private static final String DEVICE_NAME = "device_name";
    private static final String DEVICE_LOCATION = "device_location";
    private static final String DEVICE_CONTROL_URL = "device_control_url";
    private static final String DEVICE_SERVICE_TYPE = "device_service_type";
    private static final String DEVICE_RENDERING_CONTROL_URL = "device_rendering_control_url";
    private static final String DEVICE_RENDERING_SERVICE_TYPE = "device_rendering_service_type";
    private static final String MEDIA_URL = "media_url";
    private static final String RECENT_DEVICES = "recent_devices";
    // All pages share one queue so a new cast always replaces the preceding cast in order.
    private static final ExecutorService COMMAND_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService discoveryExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger discoveryGeneration = new AtomicInteger();
    private volatile boolean released;

    public DlnaCastManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void discover(DiscoveryCallback callback) {
        int generation = discoveryGeneration.incrementAndGet();
        discoveryExecutor.execute(() -> discoverDevices(generation, callback));
    }

    public void cancelDiscovery() {
        discoveryGeneration.incrementAndGet();
    }

    public void cast(Device device, String mediaUrl, long positionMs, CommandCallback callback) {
        COMMAND_EXECUTOR.execute(() -> {
            try {
                Device previousDevice = getRememberedDevice();
                executeAction(device, "SetAVTransportURI",
                        "<InstanceID>0</InstanceID>"
                                + "<CurrentURI>" + escapeXml(mediaUrl) + "</CurrentURI>"
                                + "<CurrentURIMetaData></CurrentURIMetaData>");
                if (positionMs >= 1000) {
                    try {
                        executeAction(device, "Seek",
                                "<InstanceID>0</InstanceID>"
                                        + "<Unit>REL_TIME</Unit>"
                                        + "<Target>" + formatPosition(positionMs) + "</Target>");
                    } catch (IOException ignored) {
                        // A few renderers do not implement Seek; playback can still start normally.
                    }
                }
                executeAction(device, "Play",
                        "<InstanceID>0</InstanceID><Speed>1</Speed>");
                if (previousDevice != null && !previousDevice.equals(device)) {
                    try {
                        executeAction(previousDevice, "Stop", "<InstanceID>0</InstanceID>");
                    } catch (IOException ignored) {
                        // The new cast is already active; an offline old renderer should not fail it.
                    }
                }
                rememberDevice(device, mediaUrl);
                postSuccess(callback);
            } catch (Exception error) {
                postError(callback, readableError(error));
            }
        });
    }

    public void stop(Device device, CommandCallback callback) {
        COMMAND_EXECUTOR.execute(() -> {
            try {
                executeAction(device, "Stop", "<InstanceID>0</InstanceID>");
                forgetDevice(device);
                postSuccess(callback);
            } catch (Exception error) {
                forgetDevice(device);
                postError(callback, readableError(error));
            }
        });
    }

    public void play(Device device, CommandCallback callback) {
        runCommand(device, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>", callback);
    }

    public void pause(Device device, CommandCallback callback) {
        runCommand(device, "Pause", "<InstanceID>0</InstanceID>", callback);
    }

    public void seek(Device device, long positionMs, CommandCallback callback) {
        runCommand(device, "Seek", "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>"
                + formatPosition(Math.max(0, positionMs)) + "</Target>", callback);
    }

    public void setVolume(Device device, int volume, CommandCallback callback) {
        if (device.renderingControlUrl == null || device.renderingServiceType == null) {
            postError(callback, "设备不支持音量控制");
            return;
        }
        COMMAND_EXECUTOR.execute(() -> {
            try {
                executeAction(device.renderingControlUrl, device.renderingServiceType, "SetVolume",
                        "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>"
                                + Math.max(0, Math.min(volume, 100)) + "</DesiredVolume>");
                postSuccess(callback);
            } catch (Exception error) {
                postError(callback, readableError(error));
            }
        });
    }

    public void getPlaybackStatus(Device device, PlaybackStatusCallback callback) {
        COMMAND_EXECUTOR.execute(() -> {
            try {
                String positionResponse = executeAction(device, "GetPositionInfo",
                        "<InstanceID>0</InstanceID>");
                String transportResponse = executeAction(device, "GetTransportInfo",
                        "<InstanceID>0</InstanceID>");
                postPlaybackStatus(callback, new PlaybackStatus(
                        parsePosition(findXmlValue(positionResponse, "RelTime")),
                        parsePosition(findXmlValue(positionResponse, "TrackDuration")),
                        findXmlValue(transportResponse, "CurrentTransportState"),
                        findXmlValue(positionResponse, "TrackURI")));
            } catch (Exception error) {
                postPlaybackError(callback, readableError(error));
            }
        });
    }

    public void getVolume(Device device, VolumeCallback callback) {
        if (device.renderingControlUrl == null || device.renderingServiceType == null) {
            postVolumeError(callback, "设备不支持音量控制");
            return;
        }
        COMMAND_EXECUTOR.execute(() -> {
            try {
                String response = executeAction(device.renderingControlUrl,
                        device.renderingServiceType, "GetVolume",
                        "<InstanceID>0</InstanceID><Channel>Master</Channel>");
                postVolume(callback, Integer.parseInt(findXmlValue(response, "CurrentVolume")));
            } catch (Exception error) {
                postVolumeError(callback, readableError(error));
            }
        });
    }

    private void runCommand(Device device, String action, String arguments,
                            CommandCallback callback) {
        COMMAND_EXECUTOR.execute(() -> {
            try {
                executeAction(device, action, arguments);
                postSuccess(callback);
            } catch (Exception error) {
                postError(callback, readableError(error));
            }
        });
    }

    public boolean stopRemembered(CommandCallback callback) {
        Device device = getRememberedDevice();
        if (device == null) {
            return false;
        }
        stop(device, callback);
        return true;
    }

    public void release() {
        released = true;
        cancelDiscovery();
        discoveryExecutor.shutdownNow();
        // The application-wide command queue remains alive so the next page can replace this cast.
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void discoverDevices(int generation, DiscoveryCallback callback) {
        WifiManager.MulticastLock multicastLock = null;
        DatagramSocket socket = null;
        Set<String> locations = new HashSet<>();
        boolean foundDevice = false;
        try {
            WifiManager wifiManager =
                    (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("chao-video-dlna");
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
            }

            socket = new DatagramSocket();
            socket.setSoTimeout(700);
            sendSearch(socket, "urn:schemas-upnp-org:device:MediaRenderer:1");
            sendSearch(socket, "ssdp:all");

            long deadline = System.currentTimeMillis() + SEARCH_DURATION_MS;
            byte[] buffer = new byte[8192];
            while (isCurrentSearch(generation) && System.currentTimeMillis() < deadline) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException ignored) {
                    continue;
                }
                String response = new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.UTF_8);
                String location = findHeader(response, "location");
                if (location == null || !locations.add(location)) {
                    continue;
                }
                try {
                    Device device = loadDevice(location);
                    if (device != null && isCurrentSearch(generation)) {
                        foundDevice = true;
                        postDevice(callback, device);
                    }
                } catch (Exception ignored) {
                    // A network may contain unrelated UPnP devices with unusable descriptions.
                }
            }
        } catch (Exception error) {
            if (!foundDevice && isCurrentSearch(generation)) {
                postDiscoveryError(callback, readableError(error));
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
            }
            if (isCurrentSearch(generation)) {
                postDiscoveryFinished(callback);
            }
        }
    }

    private void sendSearch(DatagramSocket socket, String searchTarget) throws IOException {
        String request = "M-SEARCH * HTTP/1.1\r\n"
                + "HOST: " + SSDP_HOST + ":" + SSDP_PORT + "\r\n"
                + "MAN: \"ssdp:discover\"\r\n"
                + "MX: 2\r\n"
                + "ST: " + searchTarget + "\r\n\r\n";
        byte[] data = request.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(SSDP_HOST), SSDP_PORT);
        socket.send(packet);
    }

    private Device loadDevice(String location) throws Exception {
        HttpURLConnection connection = openConnection(new URL(location));
        connection.setRequestMethod("GET");
        try (InputStream input = checkedInput(connection)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            disableExternalEntities(factory);
            Document document = factory.newDocumentBuilder().parse(input);
            Element renderer = findMediaRenderer(document);
            if (renderer == null) {
                return null;
            }
            String name = directChildText(renderer, "friendlyName");
            String serviceType = null;
            String controlUrl = null;
            String renderingServiceType = null;
            String renderingControlUrl = null;
            for (Element service : descendants(renderer, "service")) {
                String type = directChildText(service, "serviceType");
                if (type != null && type.contains(":service:AVTransport:")) {
                    serviceType = type;
                    controlUrl = directChildText(service, "controlURL");
                } else if (type != null && type.contains(":service:RenderingControl:")) {
                    renderingServiceType = type;
                    renderingControlUrl = directChildText(service, "controlURL");
                }
            }
            if (serviceType == null || controlUrl == null) {
                return null;
            }
            String urlBase = firstText(document, "URLBase");
            URL base = new URL(urlBase == null || urlBase.trim().isEmpty()
                    ? location : urlBase.trim());
            return new Device(name == null || name.trim().isEmpty() ? base.getHost() : name.trim(),
                    location, new URL(base, controlUrl.trim()).toString(), serviceType.trim(),
                    renderingControlUrl == null ? null
                            : new URL(base, renderingControlUrl.trim()).toString(),
                    renderingServiceType == null ? null : renderingServiceType.trim());
        } finally {
            connection.disconnect();
        }
    }

    private String executeAction(Device device, String action, String arguments) throws IOException {
        return executeAction(device.controlUrl, device.serviceType, action, arguments);
    }

    private String executeAction(String controlUrl, String serviceType, String action,
                                 String arguments) throws IOException {
        String envelope = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + "<s:Body><u:" + action + " xmlns:u=\"" + serviceType + "\">"
                + arguments + "</u:" + action + "></s:Body></s:Envelope>";
        byte[] body = envelope.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = openConnection(new URL(controlUrl));
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        connection.setRequestProperty("SOAPAction",
                "\"" + serviceType + "#" + action + "\"");
        connection.setFixedLengthStreamingMode(body.length);
        try {
            connection.getOutputStream().write(body);
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String detail = readText(connection.getErrorStream());
                throw new IOException("设备返回 " + responseCode
                        + (detail.isEmpty() ? "" : ": " + detail));
            }
            return readText(connection.getInputStream(), 16 * 1024);
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(4000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Connection", "close");
        return connection;
    }

    private InputStream checkedInput(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("设备描述请求失败: " + responseCode);
        }
        return connection.getInputStream();
    }

    private Element findMediaRenderer(Document document) {
        for (Element device : elements(document, "device")) {
            String type = directChildText(device, "deviceType");
            if (type != null && type.contains(":device:MediaRenderer:")) {
                return device;
            }
        }
        return null;
    }

    private static Iterable<Element> descendants(Element parent, String name) {
        NodeList nodes = namespacedNodes(parent, name);
        java.util.List<Element> result = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                result.add((Element) nodes.item(i));
            }
        }
        return result;
    }

    private static Iterable<Element> elements(Document document, String name) {
        NodeList nodes = namespacedNodes(document.getDocumentElement(), name);
        java.util.List<Element> result = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                result.add((Element) nodes.item(i));
            }
        }
        return result;
    }

    private static NodeList namespacedNodes(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagNameNS("*", name);
        return nodes.getLength() == 0 ? parent.getElementsByTagName(name) : nodes;
    }

    private static String directChildText(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String childName = child.getLocalName() == null ? child.getNodeName() : child.getLocalName();
            if (child instanceof Element && name.equals(childName)) {
                return child.getTextContent();
            }
        }
        return null;
    }

    private static String firstText(Document document, String name) {
        NodeList nodes = namespacedNodes(document.getDocumentElement(), name);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
            // Parser implementations vary across Android versions.
        }
    }

    private static String findHeader(String response, String requestedName) {
        String[] lines = response.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator > 0 && requestedName.equalsIgnoreCase(line.substring(0, separator).trim())) {
                String value = line.substring(separator + 1).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private static String formatPosition(long positionMs) {
        long seconds = positionMs / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String readText(InputStream input) throws IOException {
        return readText(input, 500);
    }

    private static String readText(InputStream input, int limit) throws IOException {
        if (input == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && text.length() < limit) {
                text.append(line);
            }
            return text.toString();
        }
    }

    private static String readableError(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName() : message;
    }

    private static String findXmlValue(String xml, String name) throws IOException {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "<(?:[A-Za-z0-9_-]+:)?" + name + ">([^<]*)</(?:[A-Za-z0-9_-]+:)?" + name + ">")
                .matcher(xml);
        if (!matcher.find()) {
            throw new IOException("设备响应缺少 " + name);
        }
        return matcher.group(1).trim();
    }

    static long parsePosition(String value) {
        if (value == null || value.isEmpty() || "NOT_IMPLEMENTED".equals(value)) {
            return 0;
        }
        String[] parts = value.split(":");
        if (parts.length != 3) {
            return 0;
        }
        try {
            double seconds = Double.parseDouble(parts[2]);
            return (long) ((Long.parseLong(parts[0]) * 3600
                    + Long.parseLong(parts[1]) * 60 + seconds) * 1000);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isCurrentSearch(int generation) {
        return !released && generation == discoveryGeneration.get();
    }

    public Device getRememberedDevice() {
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                CAST_SESSION, Context.MODE_PRIVATE);
        String location = preferences.getString(DEVICE_LOCATION, null);
        String controlUrl = preferences.getString(DEVICE_CONTROL_URL, null);
        String serviceType = preferences.getString(DEVICE_SERVICE_TYPE, null);
        if (location == null || controlUrl == null || serviceType == null) {
            return null;
        }
        return new Device(preferences.getString(DEVICE_NAME, location), location,
                controlUrl, serviceType,
                preferences.getString(DEVICE_RENDERING_CONTROL_URL, null),
                preferences.getString(DEVICE_RENDERING_SERVICE_TYPE, null));
    }

    public String getRememberedMediaUrl() {
        return context.getSharedPreferences(CAST_SESSION, Context.MODE_PRIVATE)
                .getString(MEDIA_URL, null);
    }

    private void rememberDevice(Device device, String mediaUrl) {
        context.getSharedPreferences(CAST_SESSION, Context.MODE_PRIVATE)
                .edit()
                .putString(DEVICE_NAME, device.name)
                .putString(DEVICE_LOCATION, device.location)
                .putString(DEVICE_CONTROL_URL, device.controlUrl)
                .putString(DEVICE_SERVICE_TYPE, device.serviceType)
                .putString(DEVICE_RENDERING_CONTROL_URL, device.renderingControlUrl)
                .putString(DEVICE_RENDERING_SERVICE_TYPE, device.renderingServiceType)
                .putString(MEDIA_URL, mediaUrl)
                .apply();
        rememberRecentDevice(device);
    }

    public List<Device> getRecentDevices() {
        List<Device> devices = new ArrayList<>();
        String saved = context.getSharedPreferences(CAST_SESSION, Context.MODE_PRIVATE)
                .getString(RECENT_DEVICES, null);
        if (saved == null) {
            return devices;
        }
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                devices.add(new Device(item.optString("name"), item.optString("location"),
                        item.optString("control"), item.optString("service"),
                        nullable(item.optString("renderingControl")),
                        nullable(item.optString("renderingService"))));
            }
        } catch (Exception ignored) {
            context.getSharedPreferences(CAST_SESSION, Context.MODE_PRIVATE)
                    .edit().remove(RECENT_DEVICES).apply();
        }
        return devices;
    }

    private void rememberRecentDevice(Device device) {
        List<Device> devices = getRecentDevices();
        devices.remove(device);
        devices.add(0, device);
        JSONArray array = new JSONArray();
        for (int i = 0; i < Math.min(devices.size(), 5); i++) {
            Device item = devices.get(i);
            try {
                array.put(new JSONObject()
                        .put("name", item.name)
                        .put("location", item.location)
                        .put("control", item.controlUrl)
                        .put("service", item.serviceType)
                        .put("renderingControl", item.renderingControlUrl)
                        .put("renderingService", item.renderingServiceType));
            } catch (Exception ignored) {
            }
        }
        context.getSharedPreferences(CAST_SESSION, Context.MODE_PRIVATE)
                .edit().putString(RECENT_DEVICES, array.toString()).apply();
    }

    private static String nullable(String value) {
        return value == null || value.isEmpty() || "null".equals(value) ? null : value;
    }

    private void forgetDevice(Device device) {
        Device rememberedDevice = getRememberedDevice();
        if (rememberedDevice != null && rememberedDevice.equals(device)) {
            context.getSharedPreferences(CAST_SESSION, Context.MODE_PRIVATE)
                    .edit()
                    .remove(DEVICE_NAME)
                    .remove(DEVICE_LOCATION)
                    .remove(DEVICE_CONTROL_URL)
                    .remove(DEVICE_SERVICE_TYPE)
                    .remove(DEVICE_RENDERING_CONTROL_URL)
                    .remove(DEVICE_RENDERING_SERVICE_TYPE)
                    .remove(MEDIA_URL)
                    .apply();
        }
    }

    private void postDevice(DiscoveryCallback callback, Device device) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onDeviceFound(device);
            }
        });
    }

    private void postDiscoveryFinished(DiscoveryCallback callback) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onFinished();
            }
        });
    }

    private void postDiscoveryError(DiscoveryCallback callback, String error) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onError(error);
            }
        });
    }

    private void postSuccess(CommandCallback callback) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> {
            if (!released) {
                callback.onSuccess();
            }
        });
    }

    private void postError(CommandCallback callback, String error) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> {
            if (!released) {
                callback.onError(error);
            }
        });
    }

    private void postPlaybackStatus(PlaybackStatusCallback callback, PlaybackStatus status) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onStatus(status);
            }
        });
    }

    private void postPlaybackError(PlaybackStatusCallback callback, String error) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onError(error);
            }
        });
    }

    private void postVolume(VolumeCallback callback, int volume) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onVolume(volume);
            }
        });
    }

    private void postVolumeError(VolumeCallback callback, String error) {
        mainHandler.post(() -> {
            if (!released) {
                callback.onError(error);
            }
        });
    }

    public interface DiscoveryCallback {
        void onDeviceFound(Device device);

        void onFinished();

        void onError(String error);
    }

    public interface CommandCallback {
        void onSuccess();

        void onError(String error);
    }

    public interface PlaybackStatusCallback {
        void onStatus(PlaybackStatus status);

        void onError(String error);
    }

    public interface VolumeCallback {
        void onVolume(int volume);

        void onError(String error);
    }

    public static final class PlaybackStatus {
        private final long positionMs;
        private final long durationMs;
        private final String state;
        private final String mediaUrl;

        PlaybackStatus(long positionMs, long durationMs, String state, String mediaUrl) {
            this.positionMs = positionMs;
            this.durationMs = durationMs;
            this.state = state;
            this.mediaUrl = mediaUrl;
        }

        public long getPositionMs() {
            return positionMs;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public boolean isPlaying() {
            return "PLAYING".equalsIgnoreCase(state);
        }

        public boolean ownsMedia(String url) {
            return url != null && !url.isEmpty() && url.equals(mediaUrl);
        }

        public boolean hasMedia() {
            return mediaUrl != null && !mediaUrl.isEmpty();
        }

        public boolean isActive() {
            return isPlaying() || "PAUSED_PLAYBACK".equalsIgnoreCase(state)
                    || "TRANSITIONING".equalsIgnoreCase(state);
        }
    }

    public static final class Device {
        private final String name;
        private final String location;
        private final String controlUrl;
        private final String serviceType;
        private final String renderingControlUrl;
        private final String renderingServiceType;

        Device(String name, String location, String controlUrl, String serviceType,
               String renderingControlUrl, String renderingServiceType) {
            this.name = name;
            this.location = location;
            this.controlUrl = controlUrl;
            this.serviceType = serviceType;
            this.renderingControlUrl = renderingControlUrl;
            this.renderingServiceType = renderingServiceType;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Device && location.equals(((Device) other).location);
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }
    }
}
