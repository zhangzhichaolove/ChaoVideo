package com.app.chao.chaoapp.playback;

import static org.junit.Assert.*;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28)
public class VideoTrackChoicesTest {
    private Tracks.Group group(String mime, int... support) {
        Format[] formats = new Format[support.length];
        for (int i = 0; i < formats.length; i++) {
            formats[i] = new Format.Builder().setId("" + i).setSampleMimeType(mime).build();
        }
        return new Tracks.Group(new TrackGroup(formats), false, support, new boolean[support.length]);
    }

    @Test public void emptyAndUnrelatedTracksDoNotExposeControls() {
        assertTrue(VideoTrackChoices.supported(Tracks.EMPTY, C.TRACK_TYPE_TEXT).isEmpty());
        Tracks tracks = new Tracks(Collections.singletonList(group(MimeTypes.VIDEO_H264, C.FORMAT_HANDLED)));
        assertTrue(VideoTrackChoices.supported(tracks, C.TRACK_TYPE_AUDIO).isEmpty());
        assertTrue(VideoTrackChoices.supported(tracks, C.TRACK_TYPE_TEXT).isEmpty());
    }

    @Test public void unsupportedAndExceedsCapabilityTracksAreExcluded() {
        Tracks tracks = new Tracks(Collections.singletonList(group(MimeTypes.AUDIO_AAC,
                C.FORMAT_UNSUPPORTED_TYPE, C.FORMAT_EXCEEDS_CAPABILITIES, C.FORMAT_HANDLED)));
        assertEquals(1, VideoTrackChoices.supported(tracks, C.TRACK_TYPE_AUDIO).size());
        assertEquals(2, VideoTrackChoices.supported(tracks, C.TRACK_TYPE_AUDIO).get(0).index);
    }

    @Test public void separateGroupsAndEmbeddedSubtitlesAreEnumerated() {
        Tracks tracks = new Tracks(Arrays.asList(group(MimeTypes.AUDIO_AAC, C.FORMAT_HANDLED),
                group(MimeTypes.APPLICATION_SUBRIP, C.FORMAT_HANDLED),
                group(MimeTypes.AUDIO_AAC, C.FORMAT_HANDLED, C.FORMAT_HANDLED)));
        assertEquals(3, VideoTrackChoices.supported(tracks, C.TRACK_TYPE_AUDIO).size());
        assertEquals(1, VideoTrackChoices.supported(tracks, C.TRACK_TYPE_TEXT).size());
    }
}
