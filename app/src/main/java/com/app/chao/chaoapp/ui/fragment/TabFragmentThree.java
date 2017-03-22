package com.app.chao.chaoapp.ui.fragment;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.RxBus;
import com.app.chao.chaoapp.utils.RxBusSubscriber;
import com.app.chao.chaoapp.utils.RxSubscriptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.Subscription;

/**
 * Created by Chao on 2017/3/21.
 */

public class TabFragmentThree extends BaseFragment {
    static TabFragmentThree fragment;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    Subscription mRxSub;

    public static TabFragmentThree newInstance() {
        //if (fragment==null)
        fragment = new TabFragmentThree();
        return fragment;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(LayoutInflater inflater) {

    }

    private void getEvent() {
        mRxSub = RxBus.getDefault().toObservableSticky(VideoRes.class)
                .subscribe(new RxBusSubscriber<VideoRes>() {
                    @Override
                    protected void onEvent(VideoRes videoRes) {
                        List<VideoInfo> list = new ArrayList<>();
                        for (int i = 0; i < videoRes.list.size(); i++) {
                            if (!TextUtils.isEmpty(videoRes.list.get(i).moreURL) && !TextUtils.isEmpty(videoRes.list.get(i).title)) {
                                VideoInfo videoInfo = videoRes.list.get(i).childList.get(0);
                                videoInfo.title = videoRes.list.get(i).title;
                                videoInfo.moreURL = videoRes.list.get(i).moreURL;
                                list.add(videoInfo);
                            }
                        }
                        //adapter.setData(list);
                    }
                });
        RxSubscriptions.add(mRxSub);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        RxSubscriptions.remove(mRxSub);
    }
}
