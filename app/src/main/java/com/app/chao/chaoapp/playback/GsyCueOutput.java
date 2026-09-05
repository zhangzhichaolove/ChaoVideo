package com.app.chao.chaoapp.playback;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.text.CueGroup;

/** One engine's cue stream, including cues received before GSY's prepared callback. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class GsyCueOutput {
    public interface Listener { void onCues(CueGroup cues); }
    private final Handler main = new Handler(Looper.getMainLooper());
    private CueGroup cues = CueGroup.EMPTY_TIME_ZERO;
    private Listener listener;
    private volatile boolean closed;

    public void onCues(CueGroup group) {
        main.post(() -> {
            if (closed) return;
            cues = group;
            if (listener != null) listener.onCues(group);
        });
    }

    /** UI-thread subscription. Detach before leaving the Activity or replacing this engine. */
    public void setListener(Listener listener) {
        this.listener = listener;
        if (listener != null) listener.onCues(closed ? CueGroup.EMPTY_TIME_ZERO : cues);
    }

    public void close() {
        closed = true; // Invalidate already queued decoder callbacks immediately, even off-main.
        main.post(() -> {
            cues = CueGroup.EMPTY_TIME_ZERO;
            if (listener != null) listener.onCues(cues);
            listener = null;
        });
    }
}
