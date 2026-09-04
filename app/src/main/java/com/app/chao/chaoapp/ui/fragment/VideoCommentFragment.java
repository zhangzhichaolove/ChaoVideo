package com.app.chao.chaoapp.ui.fragment;

import android.view.View;

import com.app.chao.chaoapp.R;

/** Displays an explicit empty state until the server provides a comments endpoint. */
public class VideoCommentFragment extends BaseFragment {
    @Override
    protected int getLayout() {
        return R.layout.fragment_video_comment;
    }

    @Override
    protected void initView(View rootView) {
        // Static empty state; no comments API is currently defined by the server.
    }
}
