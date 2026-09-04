package com.app.chao.chaoapp.cast;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DlnaCastManagerTest {
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
