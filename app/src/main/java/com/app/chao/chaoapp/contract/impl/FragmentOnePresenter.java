package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.PageInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentOneContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;
import com.app.chao.chaoapp.utils.StringUtils;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by Chao on 2017/3/14.
 */

public class FragmentOnePresenter extends RxPresenter implements FragmentOneContract.Presenter {
    FragmentOneContract.View view;

    public FragmentOnePresenter(FragmentOneContract.View view) {
        this.view = Preconditions.checkNotNull(view);
        view.setPresenter(this);
    }


    @Override
    public void start() {

    }

    @Override
    public void showContent(int page) {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoList(page)
                .compose(RxUtil.<VideoHttpResponse<PageInfo<List<VideoRes>>>>rxSchedulerHelper())
                .compose(RxUtil.<PageInfo<List<VideoRes>>>handleResult())
                .subscribe(new Action1<PageInfo<List<VideoRes>>>() {
                    @Override
                    public void call(final PageInfo<List<VideoRes>> res) {
                        if (res != null) {
                            view.showContent(res.getRecords());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void showBanner() {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoBanner()
                .compose(RxUtil.<VideoHttpResponse<List<VideoRes>>>rxSchedulerHelper())
                .compose(RxUtil.<List<VideoRes>>handleResult())
                .subscribe(new Action1<List<VideoRes>>() {
                    @Override
                    public void call(final List<VideoRes> res) {
                        if (res != null) {
                            view.showBanner(res);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }
}
