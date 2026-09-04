package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.FragmentOneAdapter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.contract.impl.FragmentTwoPresenter;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.EndlessScrollListener;
import com.app.chao.chaoapp.utils.GridSpacingItemDecoration;
import com.app.chao.chaoapp.utils.ScreenUtil;

import java.util.List;

/**
 * Created by Chao on 2017/3/20.
 */

public class TabFragmentTwo extends BaseFragment<FragmentTwoContract.Presenter> implements FragmentTwoContract.View {
    SwipeRefreshLayout materialRefreshLayout;
    RecyclerView recyclerView;
    FragmentOneAdapter adapter;
    View listState;
    ProgressBar listStateProgress;
    TextView listStateMessage;
    Button listStateRetry;
    EndlessScrollListener endlessScrollListener;

    public static TabFragmentTwo newInstance(String type) {
        TabFragmentTwo fragment = new TabFragmentTwo();
        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(View inflater) {
        inflater.findViewById(R.id.recommend_header).setVisibility(View.GONE);
        materialRefreshLayout = inflater.findViewById(R.id.refresh);
        recyclerView = inflater.findViewById(R.id.recyclerView);
        listState = inflater.findViewById(R.id.list_state);
        listStateProgress = inflater.findViewById(R.id.list_state_progress);
        listStateMessage = inflater.findViewById(R.id.list_state_message);
        listStateRetry = inflater.findViewById(R.id.list_state_retry);
        listStateRetry.setOnClickListener(view -> {
            showLoading();
            if (endlessScrollListener != null) {
                endlessScrollListener.reset();
            }
            mPresenter.onRefresh();
        });
        new FragmentTwoPresenter(this);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(
                ScreenUtil.dip2px(getContext(), 8)));
        recyclerView.setAdapter(adapter = new FragmentOneAdapter());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        endlessScrollListener = new EndlessScrollListener(gridLayoutManager,
                () -> mPresenter.loadMore());
        recyclerView.addOnScrollListener(endlessScrollListener);
        adapter.setOnItemClickListener(position ->
                JumpUtil.goGSYYVideoActivity(mContext, adapter.getItem(position)));
        listener();
    }

    private void listener() {
        materialRefreshLayout.setColorSchemeResources(R.color.DeepPink, R.color.colorPrimary);
        materialRefreshLayout.setOnRefreshListener(() -> {
            endlessScrollListener.reset();
            mPresenter.onRefresh();
        });
        materialRefreshLayout.setRefreshing(true);
        mPresenter.onRefresh();
    }

    private void close() {
        materialRefreshLayout.setRefreshing(false);
    }

    private void showLoading() {
        listState.setVisibility(View.VISIBLE);
        listStateProgress.setVisibility(View.VISIBLE);
        listStateRetry.setVisibility(View.GONE);
        listStateMessage.setText(R.string.video_loading);
    }

    private void showListState(boolean empty) {
        listState.setVisibility(empty ? View.VISIBLE : View.GONE);
        listStateProgress.setVisibility(View.GONE);
        listStateRetry.setVisibility(View.GONE);
        listStateMessage.setText(R.string.video_empty);
    }


    @Override
    public void setPresenter(FragmentTwoContract.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public String getType() {
        return getArguments().getString("type", "");
    }

    @Override
    public void showContent(List<VideoRes> list) {
        adapter.setData(list);
        endlessScrollListener.finish(list != null && !list.isEmpty());
        showListState(list == null || list.isEmpty());
        close();
    }

    @Override
    public void showMoreContent(List<VideoRes> list) {
        if (list != null) {
            adapter.addAll(list);
        }
        endlessScrollListener.finish(list != null && !list.isEmpty());
        close();
    }

    @Override
    public void refreshFailed(String message) {
        close();
        if (endlessScrollListener != null) {
            endlessScrollListener.finish(true);
        }
        if (adapter != null && adapter.getCount() > 0) {
            listState.setVisibility(View.GONE);
            return;
        }
        listState.setVisibility(View.VISIBLE);
        listStateProgress.setVisibility(View.GONE);
        listStateRetry.setVisibility(View.VISIBLE);
        listStateMessage.setText(getString(R.string.video_load_failed,
                TextUtils.isEmpty(message) ? getString(R.string.unknown_error) : message));
    }
}
