package com.app.chao.chaoapp.playback;

import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.os.Looper;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class GsyCueOutputTest {
    @Test public void earlyCuesReplayAndTextAndBitmapCuesAreNotFlattened() {
        GsyCueOutput output = new GsyCueOutput();
        Cue text = new Cue.Builder().setText("Caption").setPosition(0.25f).build();
        Cue bitmap = new Cue.Builder().setBitmap(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)).build();
        CueGroup group = new CueGroup(Arrays.asList(text, bitmap), 123L);
        output.onCues(group); Shadows.shadowOf(Looper.getMainLooper()).idle();
        List<CueGroup> received = new ArrayList<>(); output.setListener(received::add);
        assertSame(group, received.get(0)); assertEquals(0.25f, received.get(0).cues.get(0).position, 0);
        assertSame(bitmap.bitmap, received.get(0).cues.get(1).bitmap);
    }
    @Test public void closingInvalidatesQueuedDecoderCuesAndLateSubscribersSeeEmpty() {
        GsyCueOutput output = new GsyCueOutput(); List<CueGroup> received = new ArrayList<>(); output.setListener(received::add);
        output.onCues(new CueGroup(Arrays.asList(new Cue.Builder().setText("STALE").build()), 0));
        output.close(); Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertTrue(received.stream().allMatch(group -> group.cues.isEmpty()));
        output.setListener(received::add); assertTrue(received.get(received.size() - 1).cues.isEmpty());
    }
    @Test public void detachingActivityDropsQueuedCallbacksWithoutClosingThePlayingEngine() {
        GsyCueOutput output = new GsyCueOutput(); List<CueGroup> old = new ArrayList<>(); output.setListener(old::add);
        CueGroup group = new CueGroup(Arrays.asList(new Cue.Builder().setText("Current").build()), 0);
        output.onCues(group); output.setListener(null); Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, old.size()); assertTrue(old.get(0).cues.isEmpty());
        List<CueGroup> resumed = new ArrayList<>(); output.setListener(resumed::add); assertSame(group, resumed.get(0));
    }
}
