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
    //SpecialAdapter adapter;
    FragmentOneAdapter adapter;
    View listState;
    ProgressBar listStateProgress;
    TextView listStateMessage;
    Button listStateRetry;
    EndlessScrollListener endlessScrollListener;

    public static TabFragmentTwo newInstance(String type) {
        //if (fragment == null)
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
        materialRefreshLayout = inflater.findViewById(R.id.refresh);
        recyclerView = inflater.findViewById(R.id.recyclerView);
        listState = inflater.findViewById(R.id.list_state);
        listStateProgress = inflater.findViewById(R.id.list_state_progress);
        listStateMessage = inflater.findViewById(R.id.list_state_message);
        listStateRetry = inflater.findViewById(R.id.list_state_retry);
        listStateRetry.setOnClickListener(view -> {
            showLoading();
            mPresenter.onRefresh();
        });
        new FragmentTwoPresenter(this);
        //设置Item增加、移除动画
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(
                ScreenUtil.dip2px(getContext(), 8)));
//        recyclerView.setAdapter(adapter = new SpecialAdapter(getActivity(), R.layout.item_found, null));
//        adapter.setOnItemClickListener(new MultiItemTypeAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
//                JumpUtil.go2VideoListActivity(mContext, "1", adapter.getItem(position).getTitle());
//            }
//
//            @Override
//            public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
//                return false;
//            }
//        });
        recyclerView.setAdapter(adapter = new FragmentOneAdapter(getContext()));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        endlessScrollListener = new EndlessScrollListener(gridLayoutManager,
                () -> mPresenter.loadMore());
        recyclerView.addOnScrollListener(endlessScrollListener);
        adapter.setOnItemClickListener(position ->
                JumpUtil.goGSYYVideoActivity(mContext, adapter.getItem(position)));
        //getEvent();
        listener();
    }

    private void listener() {
        materialRefreshLayout.setColorSchemeResources(R.color.DeepPink, R.color.colorPrimary);
        materialRefreshLayout.setOnRefreshListener(() -> mPresenter.onRefresh());
        materialRefreshLayout.setRefreshing(true);
        mPresenter.onRefresh();
    }

    private void getEvent() {
        new FragmentTwoPresenter(this);
//        presenter.start();
//        mRxSub = RxBus.getDefault().toObservableSticky(VideoRes.class)
//                .subscribe(new RxBusSubscriber<VideoRes>() {
//                    @Override
//                    protected void onEvent(VideoRes videoRes) {
//                        List<VideoInfo> list = new ArrayList<>();
//                        for (int i = 0; videoRes != null && i < videoRes.list.size(); i++) {
//                            if (!TextUtils.isEmpty(videoRes.list.get(i).moreURL) && !TextUtils.isEmpty(videoRes.list.get(i).title)) {
//                                VideoInfo videoInfo = videoRes.list.get(i).childList.get(0);//由于此处得到的是公共实体类，如果修改也将修改整个JavaBean的数据。因为同一地址操作的是同一对象，所以不能通过进行地址给对象赋值
//                                VideoInfo clone = videoInfo.clone();
//                                clone.title = videoRes.list.get(i).title;
//                                clone.moreURL = videoRes.list.get(i).moreURL;
//                                list.add(clone);
//                            }
//                        }
//                        adapter.setData(list);
//                        close();
//                    }
//                });
//        RxSubscriptions.add(mRxSub);
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
        adapter.clear();
        if (list != null) {
            adapter.addAll(list);
        }
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
        if (endlessScrollListener != null) {
            endlessScrollListener.finish(true);
        }
    }

    @Override
    public void refreshFailed(String message) {
        close();
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

//    @Override
//    public void showContent(List<SpecialVideoData> videoRes) {
//        adapter.setData(videoRes);
//        close();
//    }

//    @Override
//    public void setPresenter(FragmentTwoContract.Presenter presenter) {
//        mPresenter = Preconditions.checkNotNull(presenter);
//        mPresenter.start();
//    }

}
