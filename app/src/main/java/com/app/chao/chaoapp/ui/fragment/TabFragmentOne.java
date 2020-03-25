package com.app.chao.chaoapp.ui.fragment;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.BannerAdapter;
import com.app.chao.chaoapp.adapter.FragmentOneAdapter;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentOneContract;
import com.app.chao.chaoapp.contract.impl.FragmentOnePresenter;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.RxSubscriptions;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.cjj.MaterialRefreshLayout;
import com.cjj.MaterialRefreshListener;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.jude.rollviewpager.RollPagerView;
import com.jude.rollviewpager.hintview.IconHintView;

import java.util.List;

import butterknife.BindView;
import rx.Subscription;

/**
 * Created by Chao on 2017/3/13.
 */

public class TabFragmentOne extends BaseFragment implements FragmentOneContract.View {
    @BindView(R.id.refresh)
    MaterialRefreshLayout materialRefreshLayout;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    RollPagerView banner;
    TextView etSearchKey;
    RelativeLayout rlGoSearch;
    List<VideoInfo> recommend;
    View headerView;
    FragmentOneAdapter adapter;
    Subscription mRxSub;
    int page = 1;
    FragmentOneContract.Presenter mPresenter;

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
        new FragmentOnePresenter(this);
        headerView = LayoutInflater.from(mContext).inflate(R.layout.recommend_header, null);
        banner = headerView.findViewById(R.id.banner);
        rlGoSearch = headerView.findViewById(R.id.rlGoSearch);
        etSearchKey = headerView.findViewById(R.id.etSearchKey);
        banner.setPlayDelay(2000);
        recyclerView.setAdapter(adapter = new FragmentOneAdapter(getContext()));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 2 : 1;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                JumpUtil.goGSYYVideoActivity(mContext, adapter.getItem(position));
            }
        });
        rlGoSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JumpUtil.goSearchActivity(mContext);
            }
        });
        //recyclerView.setErrorView(R.layout.view_error);
        //webView.loadUrl("http://www.youku.com");
        listener();
    }

    private void listener() {
        materialRefreshLayout.setShowArrow(true);//显示箭头
        materialRefreshLayout.setWaveColor(Color.parseColor("#50FF1493"));//波纹颜色
        materialRefreshLayout.setIsOverLay(true);//是否覆盖
        materialRefreshLayout.setWaveShow(true);//显示波纹
        materialRefreshLayout.setShowProgressBg(true);//显示进度背景
        materialRefreshLayout.setLoadMore(true);//加载更多
        //materialRefreshLayout.setSunStyle(true);
        materialRefreshLayout.setProgressColors(getResources().getIntArray(R.array.material_colors));//设置进度颜色
        materialRefreshLayout.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(final MaterialRefreshLayout materialRefreshLayout) {
                //RxBus.getDefault().postSticky("Refresh");
                mPresenter.showContent(1);
            }

            @Override
            public void onfinish() {
                //Toast.makeText(VideoListActivity.this, "finish", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onRefreshLoadMore(final MaterialRefreshLayout materialRefreshLayout) {
                page++;
                mPresenter.showContent(page);
            }
        });
        //materialRefreshLayout.autoRefresh();
    }

    private void close() {
        materialRefreshLayout.finishRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxSubscriptions.remove(mRxSub);
    }

    @Override
    public void onResume() {
        super.onResume();
        banner.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        banner.pause();
    }

    @Override
    public void showContent(int page, List<VideoRes> videoRes) {
        if (page <= 1) {
            adapter.clear();
        }
        if (videoRes.size() <= 0) {
            materialRefreshLayout.setLoadMore(false);
        } else {
            materialRefreshLayout.setLoadMore(true);
        }
        adapter.addAll(videoRes);
        materialRefreshLayout.finishRefresh();
        materialRefreshLayout.finishRefreshLoadMore();
    }

    @Override
    public void refreshFaild(String msg) {

    }

    @Override
    public void showBanner(final List<VideoRes> videoRes) {
        if (adapter.getHeaderCount() == 0) {
            adapter.addHeader(new RecyclerArrayAdapter.ItemView() {//Banner图
                @Override
                public View onCreateView(ViewGroup parent) {
                    banner.setHintView(new IconHintView(getContext(), R.mipmap.ic_page_indicator_focused, R.mipmap.ic_page_indicator, ScreenUtil.dip2px(getContext(), 10)));
                    banner.setHintPadding(0, 0, 0, ScreenUtil.dip2px(getContext(), 8));
                    banner.setAdapter(new BannerAdapter(getContext(), videoRes));
                    return headerView;
                }

                @Override
                public void onBindView(View headerView) {

                }
            });
        }
        close();
    }

    @Override
    public void setPresenter(FragmentOneContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
        mPresenter.showBanner();
        mPresenter.showContent(1);
    }
}