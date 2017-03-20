package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentOneContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;
import com.app.chao.chaoapp.utils.StringUtils;

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
    public void onRefresh() {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getHomePage()
                .compose(RxUtil.<VideoHttpResponse<VideoRes>>rxSchedulerHelper())
                .compose(RxUtil.<VideoRes>handleResult())
                .subscribe(new Action1<VideoRes>() {
                    @Override
                    public void call(final VideoRes res) {
                        if (res != null) {
                            view.showContent(res);
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
    public void start() {

    }
}
