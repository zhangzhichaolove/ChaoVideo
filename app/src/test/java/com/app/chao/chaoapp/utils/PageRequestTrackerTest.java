package com.app.chao.chaoapp.utils;

import static org.junit.Assert.*;
import org.junit.Test;

public class PageRequestTrackerTest {
    @Test public void refreshInvalidatesOldQuerySuccessAndFailure() {
        PageRequestTracker tracker = new PageRequestTracker();
        PageRequestTracker.Request old = tracker.refresh();
        PageRequestTracker.Request current = tracker.refresh();
        assertFalse(tracker.complete(old, true));
        assertFalse(tracker.fail(old));
        assertTrue(tracker.complete(current, true));
        assertEquals(2, tracker.next().page);
    }

    @Test public void failedSecondPageCanBeRetriedWithoutSkipping() {
        PageRequestTracker tracker = new PageRequestTracker();
        tracker.complete(tracker.refresh(), true);
        PageRequestTracker.Request second = tracker.next();
        assertNull(tracker.next());
        assertTrue(tracker.fail(second));
        assertEquals(2, tracker.next().page);
    }

    @Test public void refreshingWhileLoadingMoreCannotAppendOldPage() {
        PageRequestTracker tracker = new PageRequestTracker();
        tracker.complete(tracker.refresh(), true);
        PageRequestTracker.Request second = tracker.next();
        PageRequestTracker.Request refresh = tracker.refresh();
        assertFalse(tracker.complete(second, true));
        assertTrue(tracker.complete(refresh, true));
        assertEquals(2, tracker.next().page);
    }

    @Test public void emptyPageStopsLoadingAndDetachInvalidatesResponses() {
        PageRequestTracker tracker = new PageRequestTracker();
        tracker.complete(tracker.refresh(), false);
        assertNull(tracker.next());
        PageRequestTracker.Request pending = tracker.refresh();
        tracker.cancel();
        assertFalse(tracker.complete(pending, true));
    }
}
