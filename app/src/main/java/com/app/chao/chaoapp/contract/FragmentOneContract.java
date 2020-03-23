package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

/**
 * Created by Chao on 2017/3/14.
 */

public interface FragmentOneContract {

    interface View extends BaseView<Presenter> {

        void showContent(int page, List<VideoRes> videoRes);

        void refreshFaild(String msg);

        void showBanner(List<VideoRes> videoRes);
    }

    interface Presenter extends BasePresenter {
        void showContent(int page);

        void showBanner();
    }

}
