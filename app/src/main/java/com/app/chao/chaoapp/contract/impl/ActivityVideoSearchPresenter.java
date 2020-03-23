package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.ActivityVideoListContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by Chao on 2017/3/22.
 */

public class ActivityVideoSearchPresenter extends RxPresenter implements ActivityVideoListContract.Presenter {
    ActivityVideoListContract.View mView;
    int page = 1;

    public ActivityVideoSearchPresenter(ActivityVideoListContract.View view) {
        this.mView = view;
        mView.setPresenter(this);
    }

    @Override
    public void start() {
//        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoListByKeyWord(mView.getCatalogId(), page + "")
//                .compose(RxUtil.<VideoHttpResponse<VideoRes>>rxSchedulerHelper())
//                .compose(RxUtil.<VideoRes>handleResult())
//                .subscribe(new Action1<VideoRes>() {
//                    @Override
//                    public void call(VideoRes res) {
//                        if (res != null) {
//                            if (page == 1) {
//                                mView.showContent(res.list);
//                            } else {
//                                mView.showMoreContent(res.list);
//                            }
//                        }
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        if (page > 1) {
//                            page--;
//                        }
//                        //mView.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
//                    }
//                });
//        addSubscribe(rxSubscription);
        getVideoHomeData();
    }

    @Override
    public void getVideoHomeData() {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoBanner()
                .compose(RxUtil.<VideoHttpResponse<List<VideoRes>>>rxSchedulerHelper())
                .compose(RxUtil.<List<VideoRes>>handleResult())
                .subscribe(new Action1<List<VideoRes>>() {
                    @Override
                    public void call(final List<VideoRes> res) {
                        if (res != null) {

                            if (page == 1) {
                                mView.showContent(res);
                            } else {
                                mView.showMoreContent(res);
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        System.out.println(throwable);
                        //view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void onRefresh() {
        page = 1;
        start();
    }

    @Override
    public void loadMore() {
        page++;
        start();
    }
}
