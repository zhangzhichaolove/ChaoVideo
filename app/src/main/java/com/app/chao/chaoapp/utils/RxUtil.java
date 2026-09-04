package com.app.chao.chaoapp.utils;

import android.text.TextUtils;

import com.app.chao.chaoapp.net.ApiException;
import com.app.chao.chaoapp.net.VideoHttpResponse;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** Shared RxJava schedulers and API response validation. */
public final class RxUtil {
    private RxUtil() {
    }

    public static <T> ObservableTransformer<T, T> rxSchedulerHelper() {
        return upstream -> upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> ObservableTransformer<VideoHttpResponse<T>, T> handleResult() {
        return upstream -> upstream.flatMap(response -> {
            if (response != null && response.isSuccess()) {
                return createData(response.getResult());
            }
            String message = response == null ? null : response.getMsg();
            return Observable.error(new ApiException("*" + (TextUtils.isEmpty(message)
                    ? "服务器返回错误" : message)));
        });
    }

    public static <T> Observable<T> createData(T value) {
        return value == null ? Observable.empty() : Observable.just(value);
    }
}
