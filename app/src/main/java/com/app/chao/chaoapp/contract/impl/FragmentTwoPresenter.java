package com.app.chao.chaoapp.contract.impl;

import com.app.chao.chaoapp.base.RxPresenter;
import com.app.chao.chaoapp.bean.PageInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.FragmentTwoContract;
import com.app.chao.chaoapp.net.RetrofitHelper;
import com.app.chao.chaoapp.utils.PageRequestTracker;
import com.app.chao.chaoapp.utils.RxUtil;
import com.app.chao.chaoapp.utils.StringUtils;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;

public class FragmentTwoPresenter extends RxPresenter<FragmentTwoContract.View>
        implements FragmentTwoContract.Presenter {
    private final PageRequestTracker requests = new PageRequestTracker();
    private String query = "";

    public FragmentTwoPresenter(FragmentTwoContract.View view) {
        attachView(view);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        onRefresh();
    }

    private void load(PageRequestTracker.Request request) {
        if (request == null || mView == null) return;
        Disposable subscription = RetrofitHelper.getVideoApi().getTypeVideoList(request.page, query)
                .compose(RxUtil.rxSchedulerHelper())
                .compose(RxUtil.handleResult())
                .defaultIfEmpty(new PageInfo<>())
                .subscribe(result -> {
                    List<VideoRes> records = result.getRecords() == null
                            ? Collections.emptyList() : result.getRecords();
                    if (mView == null || !requests.complete(request, !records.isEmpty())) return;
                    if (request.page == 1) {
                        mView.showContent(records);
                    } else {
                        mView.showMoreContent(records);
                    }
                }, error -> {
                    if (mView != null && requests.fail(request)) {
                        mView.refreshFailed(StringUtils.getErrorMsg(error.getMessage()));
                    }
                });
        addSubscribe(subscription);
    }

    @Override
    public void onRefresh() {
        if (mView == null) return;
        query = mView.getType();
        unSubscribe();
        load(requests.refresh());
    }

    @Override
    public void loadMore() {
        load(requests.next());
    }

    @Override
    public void detachView() {
        requests.cancel();
        super.detachView();
    }
}
