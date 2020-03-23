package com.app.chao.chaoapp.contract.impl;

import androidx.annotation.NonNull;

import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.CommentContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;
import com.app.chao.chaoapp.utils.StringUtils;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

public class CommentPresenter extends RxPresenter implements CommentContract.Presenter {
    @NonNull
    final CommentContract.View mView;
    int page = 1;
    String mediaId = "";


    public CommentPresenter(@NonNull CommentContract.View addTaskView) {
        mView = Preconditions.checkNotNull(addTaskView);
        mView.setPresenter(this);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        page = 1;
        if (mediaId != null && !mediaId.equals("")) {
            getComment(mediaId);
        }
    }

    private void getComment(String mediaId) {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoBanner()
                .compose(RxUtil.<VideoHttpResponse<List<VideoRes>>>rxSchedulerHelper())
                .compose(RxUtil.<List<VideoRes>>handleResult())
                .subscribe(new Action1<List<VideoRes>>() {
                    @Override
                    public void call(List<VideoRes> res) {
                        if (res != null) {
                            if (page == 1) {
                                mView.showContent(res);
                            } else {
                                mView.showMoreContent(res);
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (page > 1) {
                            page--;
                        }
                        mView.refreshFaild(StringUtils.getErrorMsg(throwable.getMessage()));
                    }
                });
        addSubscribe(rxSubscription);
    }

    @Override
    public void loadMore() {
        page++;
        if (mediaId != null && mediaId.equals("")) {
            getComment(mediaId);
        }
    }

    @Override
    public void setMediaId(String id) {
        this.mediaId = id;
    }

    @Override
    public void start() {

    }
}
