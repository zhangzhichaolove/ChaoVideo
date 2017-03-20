package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

public interface VideoInfoContract {

    interface View extends BaseView<Presenter> {

        void showContent(VideoRes videoRes);

        void showError(String error);

    }

    interface Presenter extends BasePresenter {
        void getDetailData(String dataId);


    }
}