package com.app.chao.chaoapp.contract.impl;

import android.util.Log;

import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.HomeActivityContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxBus;
import com.app.chao.chaoapp.utils.RxUtil;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by Chao on 2017/3/14.
 */

public class HomeActivityPresenter extends RxPresenter implements HomeActivityContract.Presenter {
    HomeActivityContract.View view;
    Subscription refresh;
    String TAG = this.getClass().getSimpleName();

    public HomeActivityPresenter(HomeActivityContract.View view) {
        this.view = Preconditions.checkNotNull(view);
        view.setPresenter(this);
        refresh = RxBus.getDefault().toObservable(String.class).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                if (s.equals("Refresh")) {
                    start();
                }
            }
        });
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
                            Log.e(TAG, res.toString());
                            view.showBanner(res);
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
    public void start() {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoBanner()
                .compose(RxUtil.<VideoHttpResponse<List<VideoRes>>>rxSchedulerHelper())
                .compose(RxUtil.<List<VideoRes>>handleResult())
                .subscribe(new Action1<List<VideoRes>>() {
                    @Override
                    public void call(final List<VideoRes> res) {
                        if (res != null) {
                            Log.e(TAG, res.toString());
                            view.showContent(res);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void detachView() {
        super.detachView();
        if (refresh != null)
            refresh.unsubscribe();
    }
}
