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

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Chao on 2017/3/14.
 */

public class FragmentOnePresenter extends RxPresenter implements FragmentOneContract.Presenter {
    FragmentOneContract.View view;

    public FragmentOnePresenter(FragmentOneContract.View view) {
        this.view = Preconditions.checkNotNull(view);
        attachView(view);
        view.setPresenter(this);
    }


    @Override
    public void start() {

    }

    @Override
    public void showContent(int page) {
        Disposable rxSubscription = RetrofitHelper.getVideoApi().getVideoList(page)
                .compose(RxUtil.<VideoHttpResponse<PageInfo<List<VideoRes>>>>rxSchedulerHelper())
                .compose(RxUtil.<PageInfo<List<VideoRes>>>handleResult())
                .subscribe(new Consumer<PageInfo<List<VideoRes>>>() {
                    @Override
                    public void accept(final PageInfo<List<VideoRes>> res) {
                        if (res != null) {
                            view.showContent(page, res.getRecords());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void showBanner() {
        Disposable rxSubscription = RetrofitHelper.getVideoApi().getVideoBanner()
                .compose(RxUtil.<VideoHttpResponse<List<VideoRes>>>rxSchedulerHelper())
                .compose(RxUtil.<List<VideoRes>>handleResult())
                .subscribe(new Consumer<List<VideoRes>>() {
                    @Override
                    public void accept(final List<VideoRes> res) {
                        if (res != null) {
                            view.showBanner(res);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void detachView() {
        super.detachView();
        view = null;
    }
}
