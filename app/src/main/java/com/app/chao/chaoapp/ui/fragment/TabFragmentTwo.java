package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.ClassificationAdapter;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.contract.impl.FragmentTwoPresenter;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.jude.easyrecyclerview.decoration.SpaceDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Chao on 2017/3/20.
 */

public class TabFragmentTwo extends BaseFragment implements FragmentTwoContract.View {
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    ClassificationAdapter adapter;

    public static TabFragmentTwo newInstance() {
        Bundle args = new Bundle();
        TabFragmentTwo fragment = new TabFragmentTwo();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(LayoutInflater inflater) {
        new FragmentTwoPresenter(this);
        //设置Item增加、移除动画
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        SpaceDecoration itemDecoration = new SpaceDecoration(ScreenUtil.dip2px(getContext(), 8));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        itemDecoration.setPaddingEdgeSide(true);
        itemDecoration.setPaddingStart(true);
        itemDecoration.setPaddingHeaderFooter(false);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(adapter = new ClassificationAdapter(getActivity(), null));
    }

    @Override
    public void setPresenter(FragmentTwoContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
        mPresenter.start();
    }

    @Override
    public void showContent(VideoRes videoRes) {
        List<VideoInfo> list = new ArrayList<>();
        for (int i = 0; i < videoRes.list.size(); i++) {
            if (!TextUtils.isEmpty(videoRes.list.get(i).moreURL) && !TextUtils.isEmpty(videoRes.list.get(i).title)) {
                VideoInfo videoInfo = videoRes.list.get(i).childList.get(0);
                videoInfo.title = videoRes.list.get(i).title;
                videoInfo.moreURL = videoRes.list.get(i).moreURL;
                list.add(videoInfo);
            }
        }
        recyclerView.setAdapter(adapter = new ClassificationAdapter(getActivity(), list));
    }
}
