package com.app.chao.chaoapp.base;

public interface BaseView<T extends BasePresenter> {

    void setPresenter(T presenter);
}