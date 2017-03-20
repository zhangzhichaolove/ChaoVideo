package com.app.chao.chaoapp.base;

/**
 * Created by Administrator on 2017/3/9 0009.
 */

public abstract class BasePresenterImpl<T extends BaseView> implements BasePresenter {

    protected T view;

    public BasePresenterImpl(BaseView view) {
        this.view = (T) view;
        view.setPresenter(this);
    }

}
