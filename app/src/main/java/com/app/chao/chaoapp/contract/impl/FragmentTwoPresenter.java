package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.net.ServerException;
import com.app.chao.chaoapp.net.VideoHttpResponse;

import rx.functions.Func1;

/**
 * Created by Chao on 2017/3/20.
 */

public class FragmentTwoPresenter extends RxPresenter implements FragmentTwoContract.Presenter {
    FragmentTwoContract.View view;

    public FragmentTwoPresenter(FragmentTwoContract.View view) {
        this.view = view;
        view.setPresenter(this);
    }

    @Override
    public void start() {
//        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoSpecialData()
//                .compose(RxUtil.<VideoHttpResponse<List<SpecialVideoData>>>rxSchedulerHelper())
//                .compose(RxUtil.<List<SpecialVideoData>>handleResult())
//                .subscribe(new Action1<List<SpecialVideoData>>() {
//                    @Override
//                    public void call(final List<SpecialVideoData> res) {
//                        if (res != null) {
//                            view.showContent(res);
//                        }
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        //view.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
//                    }
//                });
//        addSubscribe(rxSubscription);

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

    }
}
