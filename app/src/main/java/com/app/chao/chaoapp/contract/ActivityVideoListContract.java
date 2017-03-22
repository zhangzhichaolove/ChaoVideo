package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoType;

import java.util.List;

/**
 * Created by Chao on 2017/3/22.
 */

public interface ActivityVideoListContract {

    interface View extends BaseView<Presenter> {

        String getCatalogId();

        void showContent(List<VideoType> list);

        void showMoreContent(List<VideoType> list);


    }

    interface Presenter extends BasePresenter {
        void onRefresh();

        void loadMore();
    }

}
