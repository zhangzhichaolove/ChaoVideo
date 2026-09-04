package com.app.chao.chaoapp.ui.fragment;

import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.BannerAdapter;
import com.app.chao.chaoapp.adapter.FragmentOneAdapter;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentOneContract;
import com.app.chao.chaoapp.contract.impl.FragmentOnePresenter;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.EndlessScrollListener;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

/**
 * Created by Chao on 2017/3/13.
 */

public class TabFragmentOne extends BaseFragment<FragmentOneContract.Presenter> implements FragmentOneContract.View {
    SwipeRefreshLayout materialRefreshLayout;
    RecyclerView recyclerView;
    ViewPager2 banner;
    TextView etSearchKey;
    RelativeLayout rlGoSearch;
    BannerAdapter bannerAdapter;
    FragmentOneAdapter adapter;
    int page = 1;
    View listState;
    ProgressBar listStateProgress;
    TextView listStateMessage;
    Button listStateRetry;
    EndlessScrollListener endlessScrollListener;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerAdvance = new Runnable() {
        @Override
        public void run() {
            if (bannerAdapter != null && bannerAdapter.getItemCount() > 1) {
                banner.setCurrentItem((banner.getCurrentItem() + 1) % bannerAdapter.getItemCount(), true);
                bannerHandler.postDelayed(this, 3000);
            }
        }
    };

    public static TabFragmentOne newInstance() {
        //if (fragment == null)
        TabFragmentOne fragment = new TabFragmentOne();
        return fragment;
    }


    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }


    @Override
    protected void initView(View inflater) {
        materialRefreshLayout = inflater.findViewById(R.id.refresh);
        recyclerView = inflater.findViewById(R.id.recyclerView);
        listState = inflater.findViewById(R.id.list_state);
        listStateProgress = inflater.findViewById(R.id.list_state_progress);
        listStateMessage = inflater.findViewById(R.id.list_state_message);
        listStateRetry = inflater.findViewById(R.id.list_state_retry);
        listStateRetry.setOnClickListener(view -> reload());
        banner = inflater.findViewById(R.id.banner);
        rlGoSearch = inflater.findViewById(R.id.rlGoSearch);
        etSearchKey = inflater.findViewById(R.id.etSearchKey);
        recyclerView.setAdapter(adapter = new FragmentOneAdapter(getContext()));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        endlessScrollListener = new EndlessScrollListener(gridLayoutManager, () -> {
            page++;
            mPresenter.showContent(page);
        });
        recyclerView.addOnScrollListener(endlessScrollListener);
        adapter.setOnItemClickListener(position ->
                JumpUtil.goGSYYVideoActivity(mContext, adapter.getItem(position)));
        rlGoSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JumpUtil.goSearchActivity(mContext);
            }
        });
        //recyclerView.setErrorView(R.layout.view_error);
        //webView.loadUrl("http://www.youku.com");
        new FragmentOnePresenter(this);
        listener();
    }

    private void listener() {
        materialRefreshLayout.setColorSchemeResources(R.color.DeepPink, R.color.colorPrimary);
        materialRefreshLayout.setOnRefreshListener(() -> {
            page = 1;
            mPresenter.showBanner();
            mPresenter.showContent(page);
        });
    }

    private void close() {
        materialRefreshLayout.setRefreshing(false);
    }

    private void reload() {
        showLoading();
        materialRefreshLayout.setRefreshing(true);
        page = 1;
        mPresenter.showBanner();
        mPresenter.showContent(1);
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
    public void onResume() {
        super.onResume();
        bannerHandler.postDelayed(bannerAdvance, 3000);
    }

    @Override
    public void onPause() {
        bannerHandler.removeCallbacks(bannerAdvance);
        super.onPause();
    }

    @Override
    public void showContent(int page, List<VideoRes> videoRes) {
        if (videoRes == null) {
            refreshFaild(getString(R.string.unknown_error));
            return;
        }
        if (page <= 1) {
            adapter.clear();
        }
        adapter.addAll(videoRes);
        endlessScrollListener.finish(!videoRes.isEmpty());
        showListState(adapter.getCount() == 0);
        materialRefreshLayout.setRefreshing(false);
    }

    @Override
    public void refreshFaild(String msg) {
        close();
        if (page > 1) {
            page--;
        }
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
                TextUtils.isEmpty(msg) ? getString(R.string.unknown_error) : msg));
    }

    @Override
    public void showBanner(final List<VideoRes> videoRes) {
        bannerAdapter = new BannerAdapter(requireContext(), videoRes);
        banner.setAdapter(bannerAdapter);
        bannerHandler.removeCallbacks(bannerAdvance);
        bannerHandler.postDelayed(bannerAdvance, 3000);
        close();
    }

    @Override
    public void setPresenter(FragmentOneContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
        mPresenter.showBanner();
        mPresenter.showContent(1);
    }
}
