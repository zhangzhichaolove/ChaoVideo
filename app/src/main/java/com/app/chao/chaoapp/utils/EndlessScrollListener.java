package com.app.chao.chaoapp.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Requests another page when a grid reaches its final few rows. */
public final class EndlessScrollListener extends RecyclerView.OnScrollListener {
    private final GridLayoutManager layoutManager;
    private final Runnable loadMore;
    private boolean enabled = true;
    private boolean loading;

    public EndlessScrollListener(GridLayoutManager layoutManager, Runnable loadMore) {
        this.layoutManager = layoutManager;
        this.loadMore = loadMore;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (enabled && !loading && dy > 0 && layoutManager.getItemCount() > 0
                && layoutManager.findLastVisibleItemPosition() >= layoutManager.getItemCount() - 4) {
            loading = true;
            loadMore.run();
        }
    }

    public void finish(boolean hasMore) {
        loading = false;
        enabled = hasMore;
    }
}
