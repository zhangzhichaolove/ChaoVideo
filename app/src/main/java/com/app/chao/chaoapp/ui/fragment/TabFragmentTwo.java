package com.app.chao.chaoapp.ui.fragment;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.SpecialAdapter;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.RxBus;
import com.app.chao.chaoapp.utils.RxBusSubscriber;
import com.app.chao.chaoapp.utils.RxSubscriptions;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.jude.easyrecyclerview.decoration.SpaceDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.Subscription;

/**
 * Created by Chao on 2017/3/20.
 */

public class TabFragmentTwo extends BaseFragment {
    static TabFragmentTwo fragment;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    SpecialAdapter adapter;
    Subscription mRxSub;

    public static TabFragmentTwo newInstance() {
        //if (fragment == null)
        fragment = new TabFragmentTwo();
        return fragment;
    }


    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(LayoutInflater inflater) {
        //new FragmentTwoPresenter(this);
        //设置Item增加、移除动画
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        SpaceDecoration itemDecoration = new SpaceDecoration(ScreenUtil.dip2px(getContext(), 8));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        itemDecoration.setPaddingEdgeSide(true);
        itemDecoration.setPaddingStart(true);
        itemDecoration.setPaddingHeaderFooter(false);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(adapter = new SpecialAdapter(getActivity(), R.layout.item_found, null));
        getEvent();
    }

    private void getEvent() {
        mRxSub = RxBus.getDefault().toObservableSticky(VideoRes.class)
                .subscribe(new RxBusSubscriber<VideoRes>() {
                    @Override
                    protected void onEvent(VideoRes videoRes) {
                        List<VideoInfo> list = new ArrayList<>();
                        for (int i = 0; i < videoRes.list.size(); i++) {
                            if (!TextUtils.isEmpty(videoRes.list.get(i).moreURL) && !TextUtils.isEmpty(videoRes.list.get(i).title)) {
                                VideoInfo videoInfo = videoRes.list.get(i).childList.get(0);//由于此处得到的是公共实体类，如果修改也将修改整个JavaBean的数据。因为同一地址操作的是同一对象，所以不能通过进行地址给对象赋值
                                VideoInfo clone = videoInfo.clone();
                                clone.title = videoRes.list.get(i).title;
                                clone.moreURL = videoRes.list.get(i).moreURL;
                                list.add(clone);
                            }
                        }
                        adapter.setData(list);
                    }
                });
        RxSubscriptions.add(mRxSub);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        RxSubscriptions.remove(mRxSub);
    }

//    @Override
//    public void setPresenter(FragmentTwoContract.Presenter presenter) {
//        mPresenter = Preconditions.checkNotNull(presenter);
//        mPresenter.start();
//    }

}
