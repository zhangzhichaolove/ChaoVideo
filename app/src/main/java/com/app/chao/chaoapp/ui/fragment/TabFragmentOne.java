package com.app.chao.chaoapp.ui.fragment;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.BannerAdapter;
import com.app.chao.chaoapp.adapter.FragmentOneAdapter;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.RxBus;
import com.app.chao.chaoapp.utils.RxBusSubscriber;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.jude.rollviewpager.RollPagerView;
import com.jude.rollviewpager.hintview.IconHintView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;

/**
 * Created by Chao on 2017/3/13.
 */

public class TabFragmentOne extends BaseFragment {
    static TabFragmentOne fragment;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    RollPagerView banner;
    TextView etSearchKey;
    RelativeLayout rlGoSearch;
    List<VideoInfo> recommend;
    View headerView;
    FragmentOneAdapter adapter;

    public static TabFragmentOne newInstance() {
        //if (fragment == null)
        fragment = new TabFragmentOne();
        return fragment;
    }


    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(LayoutInflater inflater) {
        //new FragmentOnePresenter(this);
        headerView = LayoutInflater.from(mContext).inflate(R.layout.recommend_header, null);
        banner = ButterKnife.findById(headerView, R.id.banner);
        rlGoSearch = ButterKnife.findById(headerView, R.id.rlGoSearch);
        etSearchKey = ButterKnife.findById(headerView, R.id.etSearchKey);
        banner.setPlayDelay(2000);
        recyclerView.setAdapter(adapter = new FragmentOneAdapter(getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                JumpUtil.goGSYYVideoActivity(mContext, adapter.getItem(position));
            }
        });
        //recyclerView.setErrorView(R.layout.view_error);
        //webView.loadUrl("http://www.youku.com");
        getEvent();
    }

    private void getEvent() {
        Observable<VideoRes> mRxSub = RxBus.getDefault().toObservableSticky(VideoRes.class);
        mRxSub.subscribe(new RxBusSubscriber<VideoRes>() {
            @Override
            protected void onEvent(final VideoRes videoRes) {
                if (videoRes != null) {
                    adapter.clear();
                    List<VideoInfo> videoInfos;
                    for (int i = 1; i < videoRes.list.size(); i++) {
                        if (videoRes.list.get(i).title.equals("精彩推荐")) {//Banner图
                            videoInfos = videoRes.list.get(i).childList;
                            adapter.addAll(videoInfos);
                            break;
                        }
                    }
                    for (int i = 1; i < videoRes.list.size(); i++) {
                        if (videoRes.list.get(i).title.equals("免费推荐")) {
                            recommend = videoRes.list.get(i).childList;
                            break;
                        }
                    }
                    if (adapter.getHeaderCount() == 0) {
                        adapter.addHeader(new RecyclerArrayAdapter.ItemView() {
                            @Override
                            public View onCreateView(ViewGroup parent) {
                                banner.setHintView(new IconHintView(getContext(), R.mipmap.ic_page_indicator_focused, R.mipmap.ic_page_indicator, ScreenUtil.dip2px(getContext(), 10)));
                                banner.setHintPadding(0, 0, 0, ScreenUtil.dip2px(getContext(), 8));
                                banner.setAdapter(new BannerAdapter(getContext(), videoRes.list.get(0).childList));
                                return headerView;
                            }

                            @Override
                            public void onBindView(View headerView) {

                            }
                        });
                    }
                }
            }
        });
    }


//    @Override
//    public void setPresenter(FragmentOneContract.Presenter presenter) {
//        mPresenter = Preconditions.checkNotNull(presenter);
//        presenter.onRefresh();
//    }

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
}
