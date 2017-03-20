package com.app.chao.chaoapp.base;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;


/**
 * View基类
 *
 * @param <T> BasePresenter
 */
public abstract class RootView<T extends BasePresenter> extends LinearLayout {
    protected boolean mActive;//是否被销毁
    protected Context mContext;
    protected T mPresenter;

    public RootView(Context context) {
        super(context);
        init();
    }

    public RootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected abstract void getLayout();

    protected abstract void initView();

    protected abstract void initEvent();

    protected abstract void initAction();

    protected void init() {
        mContext = getContext();
        getLayout();
        mActive = true;
        initView();
        initEvent();
    }

    @Override
    protected void onAttachedToWindow() {//附加到窗口中;
        super.onAttachedToWindow();
        if (mPresenter != null)
            mActive = true;
    }

    @Override
    protected void onDetachedFromWindow() {//解除窗口
        super.onDetachedFromWindow();
//        if (mPresenter != null)
//            mPresenter.detachView();
        mActive = false;
        mContext = null;
    }

    public void setPresenter(T presenter) {
        mPresenter = Preconditions.checkNotNull(presenter, "presenter is not null");
        initAction();
    }
}
