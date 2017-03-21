package com.app.chao.chaoapp.utils;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class EasyRxBus {
    private static volatile EasyRxBus mInstance;
    private final Subject<Object, Object> bus;

    private EasyRxBus() {
        bus = new SerializedSubject<>(PublishSubject.create());
    }

    //单例RxBus
    public static EasyRxBus getInstance() {
        EasyRxBus rxBus = mInstance;
        if (mInstance == null) {
            synchronized (EasyRxBus.class) {
                rxBus = mInstance;
                if (mInstance == null) {
                    rxBus = new EasyRxBus();
                    mInstance = rxBus;
                }
            }
        }
        return rxBus;
    }

    // 发送一个新事件
    public void send(Object o) {
        bus.onNext(o);
    }

    // 返回特定类型的被观察者
    public <T> Observable<T> toObservable(Class<T> eventType) {
        return bus.ofType(eventType);
    }
}