package com.app.chao.chaoapp.ui.activity;

import android.view.View;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.VideoListAdapter;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.baseadapter.recyclerview.MultiItemTypeAdapter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.ActivityVideoListContract;
import com.app.chao.chaoapp.contract.impl.ActivityVideoListPresenter;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.EndlessScrollListener;
import com.app.chao.chaoapp.utils.GridSpacingItemDecoration;
import com.app.chao.chaoapp.utils.ScreenUtil;

import java.util.List;

/**
 * Created by Chao on 2017/3/22.
 */

public class VideoListActivity extends BaseActivity<ActivityVideoListContract.Presenter> implements ActivityVideoListContract.View {
    SwipeRefreshLayout materialRefreshLayout;
    RecyclerView recyclerView;
    Toolbar toolbar;
    VideoListAdapter adapter;
    View listState;
    ProgressBar listStateProgress;
    TextView listStateMessage;
    Button listStateRetry;
    EndlessScrollListener endlessScrollListener;


    @Override
    protected int getLayout() {
        return R.layout.activity_video_list;
    }

    @Override
    protected void init() {
        materialRefreshLayout = findViewById(R.id.refresh);
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.toolbar);
        listState = findViewById(R.id.list_state);
        listStateProgress = findViewById(R.id.list_state_progress);
        listStateMessage = findViewById(R.id.list_state_message);
        listStateRetry = findViewById(R.id.list_state_retry);
        listStateRetry.setOnClickListener(view -> {
            showLoading();
            if (endlessScrollListener != null) {
                endlessScrollListener.reset();
            }
            mPresenter.onRefresh();
        });
        new ActivityVideoListPresenter(this);
        toolbar.setTitle(getIntent().getStringExtra("title"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(ScreenUtil.dip2px(this, 8)));
        recyclerView.setAdapter(adapter = new VideoListAdapter(this, R.layout.item_related, null));
        adapter.setOnItemClickListener(new MultiItemTypeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
                JumpUtil.goGSYYVideoActivity(VideoListActivity.this, adapter.getItem(position));
            }

            @Override
            public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
                return false;
            }
        });
        listener();
    }

    private void listener() {
        materialRefreshLayout.setColorSchemeResources(R.color.DeepPink, R.color.colorPrimary);
        materialRefreshLayout.setOnRefreshListener(() -> {
            endlessScrollListener.reset();
            mPresenter.onRefresh();
        });
        endlessScrollListener = new EndlessScrollListener(
                (GridLayoutManager) recyclerView.getLayoutManager(), () -> mPresenter.loadMore());
        recyclerView.addOnScrollListener(endlessScrollListener);
        materialRefreshLayout.setRefreshing(true);
        mPresenter.onRefresh();
    }


    @Override
    public void setPresenter(ActivityVideoListContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
    }


    @Override
    public String getCatalogId() {
        return getIntent().getStringExtra("catalogId");
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
        if (adapter != null && adapter.getItemCount() > 0) {
            listState.setVisibility(View.GONE);
            return;
        }
        listState.setVisibility(View.VISIBLE);
        listStateProgress.setVisibility(View.GONE);
        listStateRetry.setVisibility(View.VISIBLE);
        listStateMessage.setText(getString(R.string.video_load_failed,
                TextUtils.isEmpty(message) ? getString(R.string.unknown_error) : message));
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

    private void close() {
        materialRefreshLayout.setRefreshing(false);
    }

}
