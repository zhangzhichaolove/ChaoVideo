package com.app.chao.chaoapp.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.app.chao.chaoapp.R;

public class BaseToolBar extends Toolbar {


    EditText mSearch;
    ImageView back, img_clear;
    TextView tv_operate;

    private View mView;

    public BaseToolBar(Context context) {
        this(context, null);
    }

    public BaseToolBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseToolBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initView();
        //setContentInsetsRelative(10, 10);

    }


    private void initView() {
        if (mView == null) {
            mView = LayoutInflater.from(getContext()).inflate(R.layout.base_toolbar, null);
            back = (ImageView) mView.findViewById(R.id.back);
            img_clear = (ImageView) mView.findViewById(R.id.img_clear);
            mSearch = (EditText) mView.findViewById(R.id.edt_search);
            tv_operate = (TextView) mView.findViewById(R.id.tv_operate);

            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

            addView(mView, params);
            img_clear.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSearch.setText("");
                }
            });

            mSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (getSearchMsg().isEmpty()) {
                        hideSearchClearView();
                    } else {
                        showSearchClearView();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }
    }

    public void setSearchText(String msg) {
        mSearch.setText(msg);
    }

    public String getSearchMsg() {
        return mSearch.getText().toString().trim();
    }

    @Override
    public void setTitle(int resId) {
        setTitle(getContext().getText(resId));
    }

    @Override
    public void setTitle(CharSequence title) {
    }

    public void setRightButtonOnClickListener(OnClickListener listener) {
        tv_operate.setOnClickListener(listener);
    }

    public void setLeftButtonOnClickListener(OnClickListener listener) {
        back.setOnClickListener(listener);
    }

    public void showSearchClearView() {
        if (img_clear != null) {
            img_clear.setVisibility(VISIBLE);
        }
    }

    public void hideSearchClearView() {
        if (img_clear != null) {
            img_clear.setVisibility(GONE);
        }
    }

}  