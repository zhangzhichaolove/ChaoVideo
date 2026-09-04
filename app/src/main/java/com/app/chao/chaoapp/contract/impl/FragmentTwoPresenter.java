package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.utils.RxUtil;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Chao on 2017/3/20.
 */

public class FragmentTwoPresenter extends RxPresenter<FragmentTwoContract.View>
        implements FragmentTwoContract.Presenter {
    int page = 1;

    public FragmentTwoPresenter(FragmentTwoContract.View view) {
        attachView(view);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        Disposable rxSubscription = RetrofitHelper.getVideoApi().getTypeVideoList(page, mView.getType())
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
