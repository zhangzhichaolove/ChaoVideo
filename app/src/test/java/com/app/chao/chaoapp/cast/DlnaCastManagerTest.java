package com.app.chao.chaoapp.cast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DlnaCastManagerTest {
    @Test
    public void sessionMustMatchBothMediaAndAnActiveTransport() {
        DlnaCastManager.PlaybackStatus status = new DlnaCastManager.PlaybackStatus(
                12000, 60000, "PAUSED_PLAYBACK", "https://example.com/a.mp4");
        assertTrue(status.isActive());
        assertFalse(status.isPlaying());
        assertTrue(status.ownsMedia("https://example.com/a.mp4"));
        assertFalse(status.ownsMedia("https://example.com/b.mp4"));
        assertFalse(new DlnaCastManager.PlaybackStatus(0, 0, "STOPPED", "").isActive());
        assertFalse(new DlnaCastManager.PlaybackStatus(0, 0, "PLAYING", "").ownsMedia(""));
    }

    @Test
    public void parsesDlnaTimeWithFractionalSeconds() {
        assertEquals(3_723_500L, DlnaCastManager.parsePosition("01:02:03.500"));
    }

    @Test
    public void invalidOrUnsupportedTimeIsZero() {
        assertEquals(0L, DlnaCastManager.parsePosition("NOT_IMPLEMENTED"));
        assertEquals(0L, DlnaCastManager.parsePosition("bad"));
    }
}
