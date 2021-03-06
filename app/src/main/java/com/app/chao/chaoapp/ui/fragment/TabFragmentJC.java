package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.FragmentOneAdapter;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentOneContract;
import com.app.chao.chaoapp.contract.impl.FragmentOnePresenter;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.jude.rollviewpager.RollPagerView;
import com.jude.rollviewpager.hintview.IconHintView;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Chao on 2017/3/20.
 */

public class TabFragmentJC extends BaseFragment implements FragmentOneContract.View {
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    RollPagerView banner;
    TextView etSearchKey;
    RelativeLayout rlGoSearch;
    List<VideoInfo> recommend;
    View headerView;
    FragmentOneAdapter adapter;

    public static TabFragmentJC newInstance() {
        Bundle args = new Bundle();
        TabFragmentJC fragment = new TabFragmentJC();
        fragment.setArguments(args);
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //JumpUtil.goJCVideoActivity(mContext, adapter.getItem(position));//TODO 格式不支持，取消节操播放器
                JumpUtil.goGSYYVideoActivity(mContext, adapter.getItem(position));
            }
        });
        //recyclerView.setErrorView(R.layout.view_error);
        //webView.loadUrl("http://www.youku.com");
    }

    @Override
    public void setPresenter(FragmentOneContract.Presenter presenter) {
        mPresenter = presenter;
        presenter.showContent(1);
    }


    @Override
    public void showContent(int page, final List<VideoRes> videoRes) {
        Log.e("TAG", videoRes.toString());
        if (videoRes != null) {
            adapter.clear();
            adapter.addAll(videoRes);
            List<VideoInfo> videoInfos;
            if (adapter.getHeaderCount() == 0) {
                adapter.addHeader(new RecyclerArrayAdapter.ItemView() {
                    @Override
                    public View onCreateView(ViewGroup parent) {
                        banner.setHintView(new IconHintView(getContext(), R.mipmap.ic_page_indicator_focused, R.mipmap.ic_page_indicator, ScreenUtil.dip2px(getContext(), 10)));
                        banner.setHintPadding(0, 0, 0, ScreenUtil.dip2px(getContext(), 8));
                        //banner.setAdapter(new BannerAdapter(getContext(), videoRes.list.get(0).childList));
                        return headerView;
                    }

                    @Override
                    public void onBindView(View headerView) {

                    }
                });
            }
        }

    }

    @Override
    public void refreshFaild(String msg) {
        Log.e("TAG", msg);
    }

    @Override
    public void showBanner(List<VideoRes> videoRes) {

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
}
