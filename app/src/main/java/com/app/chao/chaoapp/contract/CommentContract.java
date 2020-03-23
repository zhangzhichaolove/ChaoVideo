package com.app.chao.chaoapp.contract;


import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

/**
 * Description: CommentContract
 */
public interface CommentContract {

    interface View extends BaseView<Presenter> {

        void refreshFaild(String msg);

        void showContent(List<VideoRes> list);

        void showMoreContent(List<VideoRes> list);
    }

    interface Presenter extends BasePresenter {

        void onRefresh();

        void loadMore();

        void setMediaId(String id);

    }
}
