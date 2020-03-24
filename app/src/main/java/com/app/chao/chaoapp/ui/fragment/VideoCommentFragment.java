package com.app.chao.chaoapp.ui.fragment;

import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.CommentAdapter;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.CommentContract;
import com.app.chao.chaoapp.contract.impl.CommentPresenter;
import com.app.chao.chaoapp.contract.impl.VideoInfoPresenter;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.jude.easyrecyclerview.decoration.SpaceDecoration;

import org.simple.eventbus.EventBus;
import org.simple.eventbus.Subscriber;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;

/**
 * Description: 详情--评论
 */
public class VideoCommentFragment extends BaseFragment<CommentContract.Presenter> implements CommentContract.View, SwipeRefreshLayout.OnRefreshListener, RecyclerArrayAdapter.OnLoadMoreListener {
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    CommentAdapter adapter;

    @Override
    protected int getLayout() {
        return R.layout.fragment_video_intro;
    }

    @Override
    protected void initView(View inflater) {
        mPresenter = new CommentPresenter(this);
        EventBus.getDefault().register(this);
//        ((CommentPresenter) mPresenter).onRefresh();
        recyclerView.setAdapter(adapter = new CommentAdapter(getContext()));
        //recyclerView.setErrorView(R.layout.view_error);
        adapter.setMore(R.layout.view_more, this);
        adapter.setNoMore(R.layout.view_nomore);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SpaceDecoration itemDecoration = new SpaceDecoration(ScreenUtil.dip2px(getContext(), 8));
        itemDecoration.setPaddingEdgeSide(true);
        itemDecoration.setPaddingStart(true);
        itemDecoration.setPaddingHeaderFooter(false);
        recyclerView.addItemDecoration(itemDecoration);
        adapter.addAll(Arrays.asList("1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


//    @Override
//    protected void initEvent() {
//        recyclerView.setRefreshListener(this);
//        adapter.setError(R.layout.view_error_footer).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                adapter.resumeMore();
//            }
//        });
//        recyclerView.getErrorView().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                recyclerView.showProgress();
//                onRefresh();
//            }
//        });
//    }

    @Override
    public void refreshFaild(String msg) {
        if (!TextUtils.isEmpty(msg))
            showError(msg);
        // recyclerView.showError();
    }


    public void clearFooter() {
        adapter.setMore(new View(mContext), this);
        adapter.setError(new View(mContext));
        adapter.setNoMore(new View(mContext));
    }

    @Override
    public void showContent(List<VideoRes> list) {
        if (list != null && list.size() < 30) {
            clearFooter();
        }
//        adapter.addAll(list);
    }

    @Override
    public void showMoreContent(List<VideoRes> list) {
//        adapter.addAll(list);
    }

    @Override
    public void setPresenter(CommentContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
    }

    @Override
    public void onLoadMore() {
        //mPresenter.loadMore();
        adapter.stopMore();
    }

    @Override
    public void onRefresh() {
        //mPresenter.onRefresh();
        adapter.clear();
    }

    public void showError(String msg) {
    }

    @Subscriber(tag = VideoInfoPresenter.Put_DataId)
    public void setData(String dataId) {
        mPresenter.setMediaId(dataId);
        mPresenter.onRefresh();
    }
}
