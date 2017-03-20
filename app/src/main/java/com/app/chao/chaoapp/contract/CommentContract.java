package com.app.chao.chaoapp.contract;


import com.app.chao.chaoapp.base.BasePresenter;
import com.app.chao.chaoapp.base.BaseView;
import com.app.chao.chaoapp.bean.VideoType;

import java.util.List;

/**
 * Description: CommentContract
 */
public interface CommentContract {

    interface View extends BaseView<Presenter> {

        void refreshFaild(String msg);

        void showContent(List<VideoType> list);

        void showMoreContent(List<VideoType> list);
    }

    interface Presenter extends BasePresenter {

        void onRefresh();

        void loadMore();

        void setMediaId(String id);

    }
}
