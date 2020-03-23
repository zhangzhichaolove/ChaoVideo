package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

/**
 * Created by Chao on 2017/3/22.
 */

public interface ActivityVideoListContract {

    interface View extends BaseView<Presenter> {

        String getCatalogId();

        void showContent(List<VideoRes> list);

        void showMoreContent(List<VideoRes> list);


    }

    interface Presenter extends BasePresenter {

        void getVideoHomeData();

        void onRefresh();

        void loadMore();
    }

}
