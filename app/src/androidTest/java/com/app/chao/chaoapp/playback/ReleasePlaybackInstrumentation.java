package com.app.chao.chaoapp.playback;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Platform-only: do not link Debug implementation or AndroidX test classes into a minified app. */
public class ReleasePlaybackInstrumentation extends Instrumentation {
    private Bundle arguments;
    private volatile Activity currentActivity;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        this.arguments = arguments;
        start();
    }
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        currentActivity = activity;
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        int code = Activity.RESULT_CANCELED;
        SharedPreferences api = getTargetContext().getSharedPreferences("api_address", 0);
        String previousApi = api.getString("video_api", null);
        SharedPreferences settings = getTargetContext().getSharedPreferences("video_playback_settings", 0);
        float previousSpeed = settings.getFloat("speed", 1f);
        int previousAspect = settings.getInt("aspect", 0);
        UiAutomation ui = getUiAutomation();
        try {
            require(api.edit().putString("video_api", arguments.getString("api_url")).commit(), "API fixture persisted");
            require(settings.edit().putFloat("speed", 1.5f).putInt("aspect", 0).commit(), "Playback settings persisted");
            runOnMainSync(() -> getTargetContext().startActivity(new Intent()
                    .setComponent(new ComponentName(getTargetContext(), "com.app.chao.chaoapp.GuideActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            click(awaitText(ui, "Release playback fixture"));
            awaitDecodedFrame();
            awaitCaption(true);
            // Full-screen GSY creates a player by reflection; verify its constructor survived R8 too.
            runOnMainSync(() -> {
                View full = currentActivity.findViewById(id("fullscreen"));
                require(full != null && full.performClick(), "Enter fullscreen");
            });
            click(awaitText(ui, "播放设置"));
            click(awaitText(ui, "播放速度"));
            AccessibilityNodeInfo speed = awaitText(ui, "1.5 倍");
            require(speed.isChecked(), "Persisted speed selected in minified UI");
            click(awaitText(ui, "2 倍"));
            require(settings.getFloat("speed", 0) == 2f, "Changed speed persisted");
            awaitCaption(true);
            click(awaitText(ui, "播放设置")); click(awaitText(ui, "字幕")); click(awaitText(ui, "关闭字幕"));
            awaitCaption(false);
            click(awaitText(ui, "播放设置")); click(awaitText(ui, "字幕")); click(awaitText(ui, "自动（媒体默认）"));
            awaitCaption(true);
            SystemClock.sleep(200);
            File dir = new File(getTargetContext().getExternalFilesDir(null), "verification-release-playback");
            require(dir.isDirectory() || dir.mkdirs(), "Evidence directory");
            Bitmap screenshot = ui.takeScreenshot();
            require(screenshot != null, "Screenshot");
            try (FileOutputStream out = new FileOutputStream(new File(dir, "player.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            screenshot.recycle();
            try (FileOutputStream out = new FileOutputStream(new File(dir, "verification.txt"))) {
                out.write(("api_fixture_displayed=true\ndecoded_texture_colors_gt_4=true\n"
                        + "explicit_relative_episode_url=true\nduration=00:30\nposition_advanced=true\nfullscreen_reflection=true\n"
                        + "restored_speed=1.5\nchanged_speed=2.0\nsubtitle_canvas_pixels=true\nsubtitle_off_auto=true\n").getBytes(StandardCharsets.UTF_8));
            }
            com.app.chao.chaoapp.ReleaseDiagnosticCheck.run(this, dir);
            result.putString("stream", "RELEASE_PLAYBACK_OK\nRELEASE_SUBTITLES_OK\nRELEASE_DIAGNOSTIC_PRIVACY_OK\n");
            code = Activity.RESULT_OK;
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));
        } finally {
            api.edit().putString("video_api", previousApi).commit();
            settings.edit().putFloat("speed", previousSpeed).putInt("aspect", previousAspect).commit();
            runOnMainSync(() -> { if (currentActivity != null) currentActivity.finish(); });
        }
        finish(code, result);
    }

    private void awaitCaption(boolean visible) {
        long end = SystemClock.elapsedRealtime() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            runOnMainSync(() -> {
                if (currentActivity == null) return;
                View caption = findCaption(currentActivity.getWindow().getDecorView());
                if (caption == null || caption.getWidth() == 0 || caption.getHeight() == 0) return;
                CharSequence description = caption.getContentDescription();
                if (visible ? !"ChaoVideo subtitle fixture".contentEquals(description == null ? "" : description)
                        : description != null && description.length() != 0) return;
                Bitmap layer = Bitmap.createBitmap(caption.getWidth(), caption.getHeight(), Bitmap.Config.ARGB_8888);
                caption.draw(new android.graphics.Canvas(layer));
                int pixels = 0;
                for (int y = 0; y < layer.getHeight(); y++) for (int x = 0; x < layer.getWidth(); x++) {
                    if (android.graphics.Color.alpha(layer.getPixel(x, y)) > 0) pixels++;
                }
                layer.recycle();
                ready.set(visible ? pixels > 20 : pixels == 0);
            });
            if (ready.get()) return;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < end);
        throw new AssertionError("Minified subtitle canvas visible=" + visible);
    }
    private View findCaption(View view) {
        if (!view.isShown()) return null;
        if (view.getId() == id("online_subtitles")) return view;
        if (view instanceof ViewGroup) for (int i = ((ViewGroup) view).getChildCount() - 1; i >= 0; i--) {
            View found = findCaption(((ViewGroup) view).getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private int id(String name) { return getTargetContext().getResources().getIdentifier(name, "id", getTargetContext().getPackageName()); }
    private void awaitDecodedFrame() {
        long end = SystemClock.elapsedRealtime() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            runOnMainSync(() -> {
                if (currentActivity == null) return;
                TextView time = currentActivity.findViewById(id("current"));
                TextView total = currentActivity.findViewById(id("total"));
                if (time == null || total == null || !"00:30".contentEquals(total.getText())
                        || "00:00".contentEquals(time.getText())) return;
                TextureView texture = findTexture(currentActivity.getWindow().getDecorView());
                if (texture == null || !texture.isAvailable()) return;
                Bitmap bitmap = texture.getBitmap(16, 16);
                if (bitmap == null) return;
                Set<Integer> colors = new HashSet<>();
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) colors.add(bitmap.getPixel(x, y));
                ready.set(colors.size() > 4);
                bitmap.recycle();
            });
            if (ready.get()) return;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < end);
        throw new AssertionError("No decoded synthetic video frame with advancing position and 30-second duration");
    }
    private TextureView findTexture(View view) {
        if (view instanceof TextureView) return (TextureView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextureView found = findTexture(((ViewGroup) view).getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }
    private AccessibilityNodeInfo awaitText(UiAutomation ui, String text) {
        long end = SystemClock.elapsedRealtime() + 15000;
        do {
            AccessibilityNodeInfo root = ui.getRootInActiveWindow();
            if (root != null) for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text))
                if (text.contentEquals(node.getText() == null ? "" : node.getText())) return node;
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < end);
        throw new AssertionError("Missing UI text: " + text);
    }
    private void click(AccessibilityNodeInfo node) {
        while (!node.isClickable() && node.getParent() != null) node = node.getParent();
        require(node.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Click UI entry");
    }
    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
