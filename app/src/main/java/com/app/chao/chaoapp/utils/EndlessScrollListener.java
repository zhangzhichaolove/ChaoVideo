package com.app.chao.chaoapp.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Requests another page when a grid reaches its final few rows. */
public final class EndlessScrollListener extends RecyclerView.OnScrollListener {
    private final GridLayoutManager layoutManager;
    private final Runnable loadMore;
    private final PagingState pagingState = new PagingState();

    public EndlessScrollListener(GridLayoutManager layoutManager, Runnable loadMore) {
        this.layoutManager = layoutManager;
        this.loadMore = loadMore;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (pagingState.shouldLoad(dy, layoutManager.getItemCount(),
                layoutManager.findLastVisibleItemPosition())) {
            loadMore.run();
        }
    }

    public void finish(boolean hasMore) {
        pagingState.finish(hasMore);
    }

    public void reset() {
        pagingState.reset();
    }
}
