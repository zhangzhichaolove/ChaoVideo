package com.app.chao.chaoapp.playback;

import androidx.media3.common.text.CueGroup;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;

/** The original and fullscreen GSY views share one engine; never subscribe per View clone. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class GsySubtitleDisplay {
    private final NormalGSYVideoPlayer root;
    private GsyCueOutput source;
    private CueGroup cues = CueGroup.EMPTY_TIME_ZERO;

    GsySubtitleDisplay(NormalGSYVideoPlayer root) { this.root = root; }

    void bind(GsyCueOutput next) {
        if (source == next) { refresh(); return; }
        if (source != null) source.setListener(null);
        source = next;
        cues = CueGroup.EMPTY_TIME_ZERO;
        refresh();
        if (next != null) next.setListener(group -> {
            if (source != next) return;
            cues = group;
            refresh();
        });
    }

    void refresh() {
        if (root instanceof PlaybackVideoPlayer) ((PlaybackVideoPlayer) root).setSubtitleCues(cues);
        if (root.getCurrentPlayer() instanceof PlaybackVideoPlayer) {
            ((PlaybackVideoPlayer) root.getCurrentPlayer()).setSubtitleCues(cues);
        }
    }
}
