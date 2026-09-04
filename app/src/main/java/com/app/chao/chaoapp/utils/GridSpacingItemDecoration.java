package com.app.chao.chaoapp.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/** Adds even spacing around grid cells without a third-party RecyclerView dependency. */
public final class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;

    public GridSpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        int half = spacing / 2;
        outRect.set(half, half, half, half);
    }
}
