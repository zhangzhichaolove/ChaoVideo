package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.RelatedAdapter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.impl.VideoInfoPresenter;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.app.chao.chaoapp.utils.StringUtils;
import com.app.chao.chaoapp.view.TextViewExpandableAnimation;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;
import com.jude.easyrecyclerview.decoration.SpaceDecoration;

import org.simple.eventbus.EventBus;
import org.simple.eventbus.Subscriber;

import butterknife.BindView;

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


    public static VideoIntroFragment newInstance(VideoRes videoInfo) {
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
        tvExpand = headerView.findViewById(R.id.tv_expand);
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
        VideoRes video = (VideoRes) getArguments().getSerializable("video");
        setData(video);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscriber(tag = VideoInfoPresenter.Refresh_Video_Info)
    public void setData(VideoRes videoInfo) {
        String dir = "导演：" + StringUtils.removeOtherCode(videoInfo.getToStar());
        String act = "主演：" + StringUtils.removeOtherCode(videoInfo.getPerformer());
        String des = dir + "\n" + act + "\n" + "简介：" + StringUtils.removeOtherCode(videoInfo.getVideoDescribe());
        tvExpand.setText(des);
//        if (videoInfo.list.size() > 1)
//            adapter.addAll(videoInfo.list.get(1).childList);
//        else
//            adapter.addAll(videoInfo.list.get(0).childList);
    }
}