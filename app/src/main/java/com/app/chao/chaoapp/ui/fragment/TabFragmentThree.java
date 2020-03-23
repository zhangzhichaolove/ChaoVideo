package com.app.chao.chaoapp.ui.fragment;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.utils.RxSubscriptions;

import butterknife.BindView;
import rx.Subscription;

/**
 * Created by Chao on 2017/3/21.
 */

public class TabFragmentThree extends BaseFragment {
    static TabFragmentThree fragment;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    Subscription mRxSub;

    public static TabFragmentThree newInstance() {
        //if (fragment==null)
        fragment = new TabFragmentThree();
        return fragment;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(View inflater) {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        RxSubscriptions.remove(mRxSub);
    }
}
