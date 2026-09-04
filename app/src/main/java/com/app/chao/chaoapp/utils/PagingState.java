package com.app.chao.chaoapp.utils;

/** Pure paging state used by scrolling lists and unit tests. */
public final class PagingState {
    private boolean enabled = true;
    private boolean loading;

    public boolean shouldLoad(int dy, int itemCount, int lastVisiblePosition) {
        if (!enabled || loading || dy <= 0 || itemCount <= 0
                || lastVisiblePosition < itemCount - 4) {
            return false;
        }
        loading = true;
        return true;
    }

    public void finish(boolean hasMore) {
        loading = false;
        enabled = hasMore;
    }

    public void reset() {
        loading = false;
        enabled = true;
    }

    boolean isLoading() {
        return loading;
    }

    boolean isEnabled() {
        return enabled;
    }
}
