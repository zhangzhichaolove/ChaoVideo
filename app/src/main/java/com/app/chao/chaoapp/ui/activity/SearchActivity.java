package com.app.chao.chaoapp.ui.activity;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.VideoListAdapter;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.baseadapter.recyclerview.MultiItemTypeAdapter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.ActivityVideoListContract;
import com.app.chao.chaoapp.contract.impl.ActivityVideoSearchPresenter;
import com.app.chao.chaoapp.listener.AppBarStateChangeListener;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.app.chao.chaoapp.utils.StatusBarUtils;
import com.app.chao.chaoapp.view.BaseToolBar;
import com.app.chao.chaoapp.view.WordWrapView;
import com.cjj.MaterialRefreshLayout;
import com.cjj.MaterialRefreshListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.jude.easyrecyclerview.decoration.SpaceDecoration;

import java.util.List;

import butterknife.BindView;

//import com.app.chao.chaoapp.bean.SearchKey;
//import com.app.chao.chaoapp.utils.RealmHelper;

/**
 * Created by Chao on 2017/3/23.
 */

public class SearchActivity extends BaseActivity<ActivityVideoListContract.Presenter> implements ActivityVideoListContract.View {
    @BindView(R.id.refresh)
    MaterialRefreshLayout materialRefreshLayout;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @BindView(R.id.toolbar)
    BaseToolBar toolbar;
    @BindView(R.id.wv_search_history)
    WordWrapView wvSearchHistory;
    @BindView(R.id.rl_history)
    LinearLayout rl_history;
    @BindView(R.id.img_search_clear)
    ImageView img_search_clear;
    VideoListAdapter adapter;
    boolean isOpen = false;

    @Override
    protected int getLayout() {
        return R.layout.activity_video_search;
    }

    @Override
    protected void init() {
        StatusBarUtils.setTranslucent(this);

        CollapsingToolbarLayout.LayoutParams lp = (CollapsingToolbarLayout.LayoutParams) toolbar.getLayoutParams();
        lp.topMargin = StatusBarUtils.getStatusBarHeight();
        toolbar.setLayoutParams(lp);

        new ActivityVideoSearchPresenter(this);

        setSupportActionBar(toolbar);
        toolbar.setLeftButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setRightButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.onRefresh();
                if (adapter.getItemCount() == 0 && !getCatalogId().isEmpty()) {
//                    SearchKey search = new SearchKey(getCatalogId(), System.currentTimeMillis());
//                    RealmHelper.getInstance().insertSearchHistory(search);
                    setHistory();
                }
            }
        });


        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        SpaceDecoration itemDecoration = new SpaceDecoration(ScreenUtil.dip2px(this, 8));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        itemDecoration.setPaddingEdgeSide(true);
        itemDecoration.setPaddingStart(true);
        itemDecoration.setPaddingHeaderFooter(false);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(adapter = new VideoListAdapter(this, R.layout.item_related, null));
        adapter.setOnItemClickListener(new MultiItemTypeAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
                JumpUtil.goGSYYVideoActivity(SearchActivity.this, adapter.getItem(position));
            }

            @Override
            public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
                return false;
            }
        });
        setHistory();
        listener();
    }


    private void listener() {
        materialRefreshLayout.setShowArrow(true);//显示箭头
        materialRefreshLayout.setWaveColor(ContextCompat.getColor(this, R.color.DeepPink));//波纹颜色
        materialRefreshLayout.setIsOverLay(false);//是否覆盖
        materialRefreshLayout.setWaveShow(true);//显示波纹
        materialRefreshLayout.setShowProgressBg(true);//显示进度背景
        materialRefreshLayout.setLoadMore(false);//加载更多
        materialRefreshLayout.setProgressColors(getResources().getIntArray(R.array.material_colors));//设置进度颜色
        materialRefreshLayout.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(final MaterialRefreshLayout materialRefreshLayout) {
                mPresenter.onRefresh();
            }

            @Override
            public void onfinish() {
                //Toast.makeText(VideoListActivity.this, "finish", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onRefreshLoadMore(final MaterialRefreshLayout materialRefreshLayout) {
                mPresenter.loadMore();
            }
        });
        //materialRefreshLayout.autoRefresh();

        appbar.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                Log.d("STATE", state.name());
                if (state == State.EXPANDED) {
                    //展开状态
                    isOpen = true;
                } else if (state == State.COLLAPSED) {
                    //折叠状态
                    isOpen = false;
                } else {
                    //中间状态
                }
            }
        });
        //appbar.setExpanded(false);//默认不展开

        img_search_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                RealmHelper.getInstance().deleteSearchHistoryAll();
                wvSearchHistory.removeAllViews();
            }
        });
    }

    private void setHistory() {
//        final List<SearchKey> searchHistory = RealmHelper.getInstance().getSearchHistoryListAll();
//        if (searchHistory != null && searchHistory.size() > 0) {
//            wvSearchHistory.removeAllViewsInLayout();
//            int size = searchHistory.size();
//            for (int i = 0; i < size; i++) {
//                final String query = searchHistory.get(i).getSearchKey();
//                TextView textView = new TextView(SearchActivity.this);
//                textView.setTextColor(Color.parseColor("#ffffff"));
//                textView.setText(query);
//                textView.setOnClickListener(onClickListener);
//                wvSearchHistory.addView(textView);
//            }
//        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            toolbar.setSearchText(((TextView) view).getText().toString().trim());
            //materialRefreshLayout.autoRefresh();
            mPresenter.onRefresh();
        }
    };

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (adapter.getItemCount() == 0) {//空列表时候,直接可以下拉刷新
//            materialRefreshLayout.dispatchTouchEvent(event);
//        } else {
//            if (isOpen) {//展开-可以刷新
//                materialRefreshLayout.dispatchTouchEvent(event);
//            } else {//关闭-先展开才能刷新
//                appbar.dispatchTouchEvent(event);
//            }
//        }
//        return super.dispatchTouchEvent(event);
//    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        Log.e("TAG", "onTouchEvent");
//        if (adapter.getItemCount() == 0) {//空列表时候,直接可以下拉刷新
//            materialRefreshLayout.onTouchEvent(event);
//        } else {
//            Log.e("TAG", isOpen + "");
//            if (isOpen) {//展开-可以刷新
//                materialRefreshLayout.onTouchEvent(event);
//            } else {//关闭-先展开才能刷新
//                appbar.onTouchEvent(event);
//            }
//        }
//        return true;
//    }

    @Override
    public void setPresenter(ActivityVideoListContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
    }


    @Override
    public String getCatalogId() {
        return toolbar.getSearchMsg();
    }

    @Override
    public void showContent(List<VideoRes> list) {
        adapter.setData(list);
        if (list != null && list.size() > 0) {
            materialRefreshLayout.setLoadMore(true);
            rl_history.setVisibility(View.GONE);
        }
        close();
    }

    @Override
    public void showMoreContent(List<VideoRes> list) {
        adapter.addAll(list);
        if (list != null && list.size() <= 0) {
            materialRefreshLayout.setLoadMore(false);
        }
        close();
    }

    private void close() {
        materialRefreshLayout.finishRefresh();
        materialRefreshLayout.finishRefreshLoadMore();
    }
}
