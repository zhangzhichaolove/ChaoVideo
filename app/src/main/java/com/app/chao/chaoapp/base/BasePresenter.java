package com.app.chao.chaoapp.base;

public interface BasePresenter<T> {
    void attachView(T view);

    void detachView();

    void start();
}