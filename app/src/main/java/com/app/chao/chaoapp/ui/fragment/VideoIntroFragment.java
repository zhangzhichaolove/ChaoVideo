package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.view.View;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.StringUtils;
import com.app.chao.chaoapp.view.TextViewExpandableAnimation;


public class VideoIntroFragment extends BaseFragment {

    TextViewExpandableAnimation tvExpand;

    @Override
    protected int getLayout() {
        return R.layout.fragment_video_intro;
    }


    public static VideoIntroFragment newInstance(VideoRes videoInfo) {
        Bundle args = new Bundle();
        args.putParcelable("video", videoInfo);
        VideoIntroFragment fragment = new VideoIntroFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initView(View inflater) {
        tvExpand = inflater.findViewById(R.id.tv_expand);
        VideoRes video = getArguments().getParcelable("video");
        setData(video);
    }

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
