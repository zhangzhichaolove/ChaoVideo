package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.HomeVideoData;
import com.app.chao.chaoapp.bean.VideoType;

import java.util.List;

/**
 * Created by Chao on 2017/3/22.
 */

public interface ActivityVideoListContract {

    interface View extends BaseView<Presenter> {

        String getCatalogId();

        void showContent(List<HomeVideoData> list);

        void showMoreContent(List<HomeVideoData> list);


    }

    interface Presenter extends BasePresenter {

        void getVideoHomeData();

        void onRefresh();

        void loadMore();
    }

}
