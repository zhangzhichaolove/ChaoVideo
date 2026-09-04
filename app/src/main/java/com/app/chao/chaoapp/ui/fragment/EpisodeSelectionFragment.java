package com.app.chao.chaoapp.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.ScreenUtil;

import java.util.ArrayList;
import java.util.List;

public class EpisodeSelectionFragment extends BaseFragment {
    private static final int COLUMN_COUNT = 4;
    private final List<TextView> episodeButtons = new ArrayList<>();
    private OnEpisodeSelectedListener listener;
    private int selectedEpisode = 1;

    public static EpisodeSelectionFragment newInstance(VideoRes video) {
        Bundle args = new Bundle();
        args.putSerializable("video", video);
        EpisodeSelectionFragment fragment = new EpisodeSelectionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEpisodeSelectedListener) {
            listener = (OnEpisodeSelectedListener) context;
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_episode_selection;
    }

    @Override
    protected void initView(View rootView) {
        GridLayout episodeGrid = rootView.findViewById(R.id.episode_grid);
        VideoRes video = (VideoRes) getArguments().getSerializable("video");
        int episodeCount = video == null ? 0 : video.getEpisodes();
        for (int episode = 1; episode <= episodeCount; episode++) {
            episodeGrid.addView(createEpisodeButton(episodeGrid, episode));
        }
        updateSelection();
    }

    private TextView createEpisodeButton(GridLayout grid, int episode) {
        TextView button = new TextView(requireContext());
        button.setText(getString(R.string.episode_number, episode));
        button.setTextSize(16);
        button.setGravity(android.view.Gravity.CENTER);
        button.setBackgroundResource(R.drawable.episode_button_background);
        button.setTextColor(getResources().getColorStateList(R.color.episode_button_text, null));
        button.setClickable(true);
        button.setFocusable(true);

        int margin = ScreenUtil.dip2px(requireContext(), 6);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ScreenUtil.dip2px(requireContext(), 48);
        params.columnSpec = GridLayout.spec((episode - 1) % COLUMN_COUNT, 1f);
        params.rowSpec = GridLayout.spec((episode - 1) / COLUMN_COUNT);
        params.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(params);

        button.setOnClickListener(view -> {
            selectedEpisode = episode;
            updateSelection();
            if (listener != null) {
                listener.onEpisodeSelected(episode);
            }
        });
        episodeButtons.add(button);
        return button;
    }

    private void updateSelection() {
        for (int i = 0; i < episodeButtons.size(); i++) {
            episodeButtons.get(i).setSelected(i + 1 == selectedEpisode);
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    public interface OnEpisodeSelectedListener {
        void onEpisodeSelected(int episode);
    }
}
