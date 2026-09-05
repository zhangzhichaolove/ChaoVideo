package com.app.chao.chaoapp.utils;

/** Binds a response to one refresh generation and advances pages only on success. */
public final class PageRequestTracker {
    private int generation;
    private int loadedPage;
    private boolean hasMore = true;
    private Request active;

    public Request refresh() {
        generation++;
        loadedPage = 0;
        hasMore = true;
        active = new Request(generation, 1);
        return active;
    }

    public Request next() {
        if (active != null || !hasMore) return null;
        active = new Request(generation, loadedPage + 1);
        return active;
    }

    public boolean complete(Request request, boolean more) {
        if (!isCurrent(request)) return false;
        loadedPage = request.page;
        hasMore = more;
        active = null;
        return true;
    }

    public boolean fail(Request request) {
        if (!isCurrent(request)) return false;
        active = null;
        return true;
    }

    public void cancel() {
        generation++;
        active = null;
    }

    private boolean isCurrent(Request request) {
        return request != null && active == request && request.generation == generation;
    }

    public static final class Request {
        private final int generation;
        public final int page;

        private Request(int generation, int page) {
            this.generation = generation;
            this.page = page;
        }
    }
}
