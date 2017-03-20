package com.app.chao.chaoapp.contract.impl;

import android.support.annotation.NonNull;

import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.CommentContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.net.VideoHttpResponse;
import com.app.chao.chaoapp.utils.RxUtil;
import com.app.chao.chaoapp.utils.StringUtils;

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
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getCommentList(mediaId, page + "")
                .compose(RxUtil.<VideoHttpResponse<VideoRes>>rxSchedulerHelper())
                .compose(RxUtil.<VideoRes>handleResult())
                .subscribe(new Action1<VideoRes>() {
                    @Override
                    public void call(VideoRes res) {
                        if (res != null) {
                            if (page == 1) {
                                mView.showContent(res.list);
                            } else {
                                mView.showMoreContent(res.list);
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
