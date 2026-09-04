package com.app.chao.chaoapp.ui.activity;

import android.util.Log;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;

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

//import com.app.chao.chaoapp.bean.SearchKey;
//import com.app.chao.chaoapp.utils.RealmHelper;

/**
 * Created by Chao on 2017/3/23.
 */

public class SearchActivity extends BaseActivity<ActivityVideoListContract.Presenter> implements ActivityVideoListContract.View {
    MaterialRefreshLayout materialRefreshLayout;
    RecyclerView recyclerView;
    AppBarLayout appbar;
    BaseToolBar toolbar;
    WordWrapView wvSearchHistory;
    LinearLayout rl_history;
    ImageView img_search_clear;
    VideoListAdapter adapter;
    View listState;
    ProgressBar listStateProgress;
    TextView listStateMessage;
    Button listStateRetry;
    boolean isOpen = false;

    @Override
    protected int getLayout() {
        return R.layout.activity_video_search;
    }

    @Override
    protected void init() {
        materialRefreshLayout = findViewById(R.id.refresh);
        recyclerView = findViewById(R.id.recyclerView);
        appbar = findViewById(R.id.appbar);
        toolbar = findViewById(R.id.toolbar);
        wvSearchHistory = findViewById(R.id.wv_search_history);
        rl_history = findViewById(R.id.rl_history);
        img_search_clear = findViewById(R.id.img_search_clear);
        listState = findViewById(R.id.search_list_state);
        listStateProgress = findViewById(R.id.list_state_progress);
        listStateMessage = findViewById(R.id.list_state_message);
        listStateRetry = findViewById(R.id.list_state_retry);
        listStateRetry.setOnClickListener(view -> requestSearch());
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
                requestSearch();
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
        materialRefreshLayout.setProgressColors(getResources().getIntArray(com.cjj.R.array.material_colors));//设置进度颜色
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
        showListState(list == null || list.isEmpty());
        close();
    }

    @Override
    public void showMoreContent(List<VideoRes> list) {
        if (list != null) {
            adapter.addAll(list);
        }
        if (list != null && list.size() <= 0) {
            materialRefreshLayout.setLoadMore(false);
        }
        close();
    }

    @Override
    public void refreshFailed(String message) {
        close();
        listState.setVisibility(View.VISIBLE);
        listStateProgress.setVisibility(View.GONE);
        listStateRetry.setVisibility(View.VISIBLE);
        listStateMessage.setText(getString(R.string.video_load_failed,
                TextUtils.isEmpty(message) ? getString(R.string.unknown_error) : message));
    }

    private void requestSearch() {
        if (TextUtils.isEmpty(getCatalogId())) {
            return;
        }
        listState.setVisibility(View.VISIBLE);
        listStateProgress.setVisibility(View.VISIBLE);
        listStateRetry.setVisibility(View.GONE);
        listStateMessage.setText(R.string.video_loading);
        rl_history.setVisibility(View.GONE);
        mPresenter.onRefresh();
    }

    private void showListState(boolean empty) {
        listState.setVisibility(empty ? View.VISIBLE : View.GONE);
        listStateProgress.setVisibility(View.GONE);
        listStateRetry.setVisibility(View.GONE);
        listStateMessage.setText(R.string.video_empty);
    }

    private void close() {
        materialRefreshLayout.finishRefresh();
        materialRefreshLayout.finishRefreshLoadMore();
    }
}
