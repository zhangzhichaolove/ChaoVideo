package com.app.chao.chaoapp.net;

import com.app.chao.chaoapp.bean.PageInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoSource;
import java.util.List;
import io.reactivex.rxjava3.core.Observable;

/** Bind provenance to the API instance that made the request, not the setting at response time. */
public final class SourcedVideoApi implements VideoApis {
    private final VideoApis delegate;
    private final String baseUrl;

    public SourcedVideoApi(VideoApis delegate, String baseUrl) {
        this.delegate = delegate;
        this.baseUrl = VideoSource.normalize(baseUrl);
    }

    @Override public Observable<VideoHttpResponse<List<VideoRes>>> getVideoBanner() {
        return delegate.getVideoBanner().map(response -> {
            bind(response.getResult());
            return response;
        });
    }

    @Override public Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getVideoList(int page) {
        return delegate.getVideoList(page).map(this::bindPage);
    }

    @Override public Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getSearchVideoList(int page, String query) {
        return delegate.getSearchVideoList(page, query).map(this::bindPage);
    }

    @Override public Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getTypeVideoList(int page, String query) {
        return delegate.getTypeVideoList(page, query).map(this::bindPage);
    }

    private VideoHttpResponse<PageInfo<List<VideoRes>>> bindPage(VideoHttpResponse<PageInfo<List<VideoRes>>> response) {
        if (response.getResult() != null) bind(response.getResult().getRecords());
        return response;
    }

    private void bind(List<VideoRes> videos) {
        if (videos != null) for (VideoRes video : videos) {
            if (video != null) {
                video.bindApiSource(baseUrl);
                video.validateEpisodeUrls();
            }
        }
    }
}
