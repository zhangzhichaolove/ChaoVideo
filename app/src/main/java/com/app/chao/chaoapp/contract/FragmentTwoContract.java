package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

/**
 * Created by Chao on 2017/3/14.
 */

public interface FragmentTwoContract {

    interface View extends BaseView<Presenter> {

        void showContent(VideoRes videoRes);

    }

    interface Presenter extends BasePresenter {
        void onRefresh();
    }

}
