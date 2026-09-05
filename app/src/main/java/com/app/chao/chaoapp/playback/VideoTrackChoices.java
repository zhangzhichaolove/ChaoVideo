package com.app.chao.chaoapp.playback;

import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import java.util.ArrayList;
import java.util.List;

/** Enumerate only tracks the current decoder can select, including separate adaptive groups. */
@androidx.annotation.OptIn(markerClass = UnstableApi.class)
public final class VideoTrackChoices {
    public final Tracks.Group group;
    public final int index;

    private VideoTrackChoices(Tracks.Group group, int index) {
        this.group = group;
        this.index = index;
    }

    public static List<VideoTrackChoices> supported(Tracks tracks, int type) {
        List<VideoTrackChoices> choices = new ArrayList<>();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != type) continue;
            for (int i = 0; i < group.length; i++) {
                if (group.isTrackSupported(i)) choices.add(new VideoTrackChoices(group, i));
            }
        }
        return choices;
    }
}
