package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.ActivityVideoListContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;

import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Chao on 2017/3/22.
 */

public class ActivityVideoSearchPresenter extends RxPresenter implements ActivityVideoListContract.Presenter {
    ActivityVideoListContract.View mView;
    int page = 1;

    public ActivityVideoSearchPresenter(ActivityVideoListContract.View view) {
        this.mView = view;
        attachView(view);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        Disposable rxSubscription = RetrofitHelper.getVideoApi().getSearchVideoList(page, mView.getCatalogId())
                .compose(RxUtil.rxSchedulerHelper())
                .compose(RxUtil.handleResult())
                .subscribe(res -> {
                    if (res != null) {
                        if (page == 1) {
                            mView.showContent(res.getRecords());
                        } else {
                            mView.showMoreContent(res.getRecords());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        if (page > 1) {
                            page--;
                        }
                        if (mView != null) {
                            mView.refreshFailed(com.app.chao.chaoapp.utils.StringUtils
                                    .getErrorMsg(throwable.getMessage()));
                        }
                    }
                });
        addSubscribe(rxSubscription);
        //getVideoHomeData();
    }

    @Override
    public void getVideoHomeData() {
        Disposable rxSubscription = RetrofitHelper.getVideoApi().getVideoBanner()
                .compose(RxUtil.<VideoHttpResponse<List<VideoRes>>>rxSchedulerHelper())
                .compose(RxUtil.<List<VideoRes>>handleResult())
                .subscribe(new Consumer<List<VideoRes>>() {
                    @Override
                    public void accept(final List<VideoRes> res) {
                        if (res != null) {

                            if (page == 1) {
                                mView.showContent(res);
                            } else {
                                mView.showMoreContent(res);
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        if (mView != null) {
                            mView.refreshFailed(com.app.chao.chaoapp.utils.StringUtils
                                    .getErrorMsg(throwable.getMessage()));
                        }
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

    @Override
    public void detachView() {
        super.detachView();
        mView = null;
    }
}
