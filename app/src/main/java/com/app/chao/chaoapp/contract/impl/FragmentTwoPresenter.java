package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.ServerException;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;

import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Chao on 2017/3/20.
 */

public class FragmentTwoPresenter extends RxPresenter implements FragmentTwoContract.Presenter {
    FragmentTwoContract.View view;
    int page = 1;

    public FragmentTwoPresenter(FragmentTwoContract.View view) {
        this.view = view;
        view.setPresenter(this);
    }

    @Override
    public void start() {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getTypeVideoList(page, view.getType())
                .compose(RxUtil.rxSchedulerHelper())
                .compose(RxUtil.handleResult())
                .subscribe(res -> {
                    if (res != null) {
                        if (page == 1) {
                            view.showContent(res.getRecords());
                        } else {
                            view.showMoreContent(res.getRecords());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (page > 1) {
                            page--;
                        }
                        //mView.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);

    }

    private class ServerResultFunc<T> implements Func1<VideoHttpResponse<T>, T> {
        @Override
        public T call(VideoHttpResponse<T> httpResult) {
            if (httpResult.isSuccess()) {
                throw new ServerException(400, httpResult.getMsg());
            }
            return httpResult.getResult();
        }
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
