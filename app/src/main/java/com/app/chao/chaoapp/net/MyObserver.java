package com.app.chao.chaoapp.net;

import rx.Subscriber;

public abstract class MyObserver<T> extends Subscriber<T> {

    @Override
    public void onError(Throwable e) {
//        e.printStackTrace();
        if (e instanceof ApiException) {
            onError((ApiException) e);
        } else {
            onError(new ApiException(e.getMessage()));
        }
    }

    /**
     * 错误回调
     */
    protected abstract void onError(ApiException ex);
}