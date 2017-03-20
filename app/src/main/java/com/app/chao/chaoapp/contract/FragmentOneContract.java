package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

/**
 * Created by Chao on 2017/3/14.
 */

public interface FragmentOneContract {

    interface View extends BaseView<Presenter> {

        void showContent(VideoRes videoRes);

        void refreshFaild(String msg);

        void stopBanner(boolean stop);
    }

    interface Presenter extends BasePresenter {
        void onRefresh();
    }

}
