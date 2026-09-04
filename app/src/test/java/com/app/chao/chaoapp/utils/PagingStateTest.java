package com.app.chao.chaoapp.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PagingStateTest {
    @Test
    public void loadsOnceNearEndUntilRequestFinishes() {
        PagingState state = new PagingState();
        assertTrue(state.shouldLoad(5, 20, 16));
        assertFalse(state.shouldLoad(5, 20, 19));
        state.finish(true);
        assertTrue(state.shouldLoad(5, 20, 19));
    }

    @Test
    public void disablesAfterLastPageAndResetEnablesRefresh() {
        PagingState state = new PagingState();
        assertTrue(state.shouldLoad(1, 10, 9));
        state.finish(false);
        assertFalse(state.shouldLoad(1, 10, 9));
        state.reset();
        assertTrue(state.shouldLoad(1, 10, 9));
    }

    @Test
    public void ignoresEmptyOrUpwardScrolls() {
        PagingState state = new PagingState();
        assertFalse(state.shouldLoad(-1, 20, 19));
        assertFalse(state.shouldLoad(1, 0, 0));
        assertFalse(state.isLoading());
        assertTrue(state.isEnabled());
    }
}
