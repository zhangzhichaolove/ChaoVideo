package com.app.chao.chaoapp.contract.impl;

import androidx.annotation.NonNull;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.VideoInfoContract;
import com.app.chao.chaoapp.utils.RxUtil;

import org.simple.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

public class VideoInfoPresenter extends RxPresenter implements VideoInfoContract.Presenter {
    public final static String Refresh_Video_Info = "Refresh_Video_Info";
    public final static String Put_DataId = "Put_DataId";
    final int WAIT_TIME = 200;

    @NonNull
    VideoInfoContract.View mView;
    String dataId = "";
    String pic = "";
    VideoRes result;

    public VideoInfoPresenter(@NonNull VideoInfoContract.View mView, VideoInfo videoInfo) {
        this.mView = mView;
        mView.setPresenter(this);
        this.dataId = videoInfo.dataId;
        this.pic = videoInfo.pic;
        getDetailData(videoInfo.dataId);
        putMediaId();
    }

    @Override
    public void getDetailData(String dataId) {
//        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoList(dataId)
//                .compose(RxUtil.<VideoHttpResponse<VideoRes>>rxSchedulerHelper())
//                .compose(RxUtil.<VideoRes>handleResult())
//                .subscribe(new Action1<VideoRes>() {
//                    @Override
//                    public void call(final VideoRes res) {
//                        if (res != null) {
//                            mView.showContent(res);
//                            result = res;
//                            postData();
////                            insertRecord();
//                        }
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        mView.showError("数据加载失败");
//                    }
//                }, new Action0() {
//                    @Override
//                    public void call() {
//                        //mView.hidLoading();
//                    }
//                });
//        addSubscribe(rxSubscription);
    }

    private void postData() {
        Subscription rxSubscription = Observable.timer(WAIT_TIME, TimeUnit.MILLISECONDS)
                .compose(RxUtil.<Long>rxSchedulerHelper())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        EventBus.getDefault().post(result, Refresh_Video_Info);
                    }
                });
        addSubscribe(rxSubscription);
    }

    private void putMediaId() {
        Subscription rxSubscription = Observable.timer(WAIT_TIME, TimeUnit.MILLISECONDS)
                .compose(RxUtil.<Long>rxSchedulerHelper())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        EventBus.getDefault().post(dataId, Put_DataId);
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void start() {

    }


}