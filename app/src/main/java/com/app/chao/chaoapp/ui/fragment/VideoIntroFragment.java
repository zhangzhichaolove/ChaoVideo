package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.RelatedAdapter;
import com.app.chao.chaoapp.bean.HomeVideoData;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.impl.VideoInfoPresenter;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.app.chao.chaoapp.utils.StringUtils;
import com.app.chao.chaoapp.view.TextViewExpandableAnimation;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.jude.easyrecyclerview.decoration.SpaceDecoration;

import org.simple.eventbus.EventBus;
import org.simple.eventbus.Subscriber;

import java.io.Serializable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoIntroFragment extends BaseFragment {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    TextViewExpandableAnimation tvExpand;
    View headerView;

    RelatedAdapter adapter;

    @Override
    protected int getLayout() {
        return R.layout.fragment_video_intro;
    }


    public static VideoIntroFragment newInstance(HomeVideoData videoInfo) {
        Bundle args = new Bundle();
        args.putSerializable("video", videoInfo);
        VideoIntroFragment fragment = new VideoIntroFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initView(View inflater) {
        EventBus.getDefault().register(this);

        headerView = LayoutInflater.from(mContext).inflate(R.layout.intro_header, null);
        tvExpand = ButterKnife.findById(headerView, R.id.tv_expand);
        recyclerView.setAdapter(adapter = new RelatedAdapter(getContext()));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, 3);
        gridLayoutManager.setSpanSizeLookup(adapter.obtainGridSpanSizeLookUp(3));
        recyclerView.setLayoutManager(gridLayoutManager);
        SpaceDecoration itemDecoration = new SpaceDecoration(ScreenUtil.dip2px(getContext(), 8));
        itemDecoration.setPaddingEdgeSide(true);
        itemDecoration.setPaddingStart(true);
        itemDecoration.setPaddingHeaderFooter(false);
        recyclerView.addItemDecoration(itemDecoration);
        adapter.setOnItemClickListener(new RecyclerArrayAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //JumpUtil.goGSYYVideoActivity(getContext(), adapter.getItem(position));
                getActivity().finish();
            }
        });
        adapter.addHeader(new RecyclerArrayAdapter.ItemView() {
            @Override
            public View onCreateView(ViewGroup parent) {
                return headerView;
            }

            @Override
            public void onBindView(View headerView) {

            }
        });
        HomeVideoData video = (HomeVideoData) getArguments().getSerializable("video");
        setData(video);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscriber(tag = VideoInfoPresenter.Refresh_Video_Info)
    public void setData(HomeVideoData videoInfo) {
        String dir = "导演：" + StringUtils.removeOtherCode("Mr·Zhang");
        String act = "主演：" + StringUtils.removeOtherCode("Mr·Zhang");
        String des = dir + "\n" + act + "\n" + "简介：" + StringUtils.removeOtherCode(videoInfo.getDescription());
        tvExpand.setText(des);
//        if (videoInfo.list.size() > 1)
//            adapter.addAll(videoInfo.list.get(1).childList);
//        else
//            adapter.addAll(videoInfo.list.get(0).childList);
    }
}