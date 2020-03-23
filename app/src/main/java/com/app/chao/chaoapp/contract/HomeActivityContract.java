package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

/**
 * Created by Chao on 2017/3/14.
 */

public interface HomeActivityContract {

    interface View extends BaseView<Presenter> {

        void showContent(List<VideoRes> videoRes);

        void showBanner(List<VideoRes> videoRes);

    }

    interface Presenter extends BasePresenter {
        void getVideoHomeData();
    }

}
