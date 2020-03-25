package com.app.chao.chaoapp.contract;

import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

/**
 * Created by Chao on 2017/3/14.
 */

public interface FragmentTwoContract {

    interface View extends BaseView<Presenter> {

        String getType();

        void showContent(List<VideoRes> list);

        void showMoreContent(List<VideoRes> list);
    }

    interface Presenter extends BasePresenter {
        void onRefresh();

        void loadMore();
    }

}
