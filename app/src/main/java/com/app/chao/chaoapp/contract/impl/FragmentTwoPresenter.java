package com.app.chao.chaoapp.contract.impl;

import android.util.Log;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.HomeVideoData;
import com.app.chao.chaoapp.bean.SpecialVideoData;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.net.ApiException;
import com.app.chao.chaoapp.net.HttpMethods;
import com.app.chao.chaoapp.net.MyObserver;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.ServerException;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
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
//        RetrofitHelper.getVideoApi().getHomePage()
//                .map(new ServerResultFunc<VideoRes>())
//                .subscribeOn(Schedulers.io())
//                .unsubscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new MyObserver<VideoRes>() {
//                    @Override
//                    protected void onError(ApiException ex) {
//                        //mView.refreshFaild(ex.getDisplayMessage());
//                    }
//
//                    @Override
//                    public void onCompleted() {
//                    }
//
//                    @Override
//                    public void onNext(VideoRes res) {
//                        if (res != null) {
//                            view.showContent(res);
//                        }
//                    }
//                });



//        HttpMethods.getInstance().queryClassification()
//                .subscribe(new MyObserver<VideoRes>() {
//                    @Override
//                    protected void onError(ApiException ex) {
//                        //mView.refreshFaild(ex.getDisplayMessage());
//                    }
//
//                    @Override
//                    public void onCompleted() {
//                    }
//
//                    @Override
//                    public void onNext(VideoRes res) {
//                        if (res != null) {
//                            view.showContent(res);
//                        }
//                    }
//                });

        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoSpecialData()
                .compose(RxUtil.<VideoHttpResponse<List<SpecialVideoData>>>rxSchedulerHelper())
                .compose(RxUtil.<List<SpecialVideoData>>handleResult())
                .subscribe(new Action1<List<SpecialVideoData>>() {
                    @Override
                    public void call(final List<SpecialVideoData> res) {
                        if (res != null) {
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

    private class ServerResultFunc<T> implements Func1<VideoHttpResponse<T>, T> {
        @Override
        public T call(VideoHttpResponse<T> httpResult) {
            if (httpResult.getCode() != 200) {
                throw new ServerException(httpResult.getCode(), httpResult.getMessage());
            }
            return httpResult.getResult();
        }
    }

    @Override
    public void onRefresh() {

    }
}
