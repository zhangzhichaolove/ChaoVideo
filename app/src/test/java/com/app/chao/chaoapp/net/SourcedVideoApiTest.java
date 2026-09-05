package com.app.chao.chaoapp.net;

import static org.junit.Assert.*;
import com.app.chao.chaoapp.bean.PageInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class SourcedVideoApiTest {
    @Test public void lateResponseBelongsToTheApiInstanceThatIssuedTheRequest() {
        FakeApi delegate = new FakeApi();
        VideoApis a = new SourcedVideoApi(delegate, "https://example.com/a/");
        var observer = a.getVideoList(2).test();
        ApiAddressManager.saveBaseUrl("https://example.com/b/");
        VideoRes video = new VideoRes(); video.setId("same-id"); video.setVideo("movie.mp4");
        delegate.page.onNext(page(video));
        observer.assertValueCount(1).assertNoErrors();
        assertEquals("https://example.com/a/movie.mp4", video.getVideo());
        assertEquals(VideoSource.apiId("https://example.com/a/"), video.getSourceId());
        assertEquals(2, delegate.lastPage);
    }

    @Test public void bannerSearchAndCategoryAlsoBindProvenanceAndPreserveEmptyPages() {
        FakeApi delegate = new FakeApi();
        VideoApis api = new SourcedVideoApi(delegate, "https://example.com/base/");
        var banner = api.getVideoBanner().test();
        VideoRes hero = new VideoRes(); hero.setVideo("hero.mp4");
        VideoHttpResponse<List<VideoRes>> response = new VideoHttpResponse<>();
        response.setResult(Arrays.asList(hero, null));
        delegate.banner.onNext(response);
        banner.assertValueCount(1).assertNoErrors();
        assertEquals("https://example.com/base/hero.mp4", hero.getVideo());
        var search = api.getSearchVideoList(3, "query").test();
        var category = api.getTypeVideoList(3, "genre").test();
        VideoRes result = new VideoRes(); result.setVideo("result.mp4");
        delegate.page.onNext(page(result));
        delegate.page.onNext(new VideoHttpResponse<>());
        search.assertValueCount(2).assertNoErrors();
        category.assertValueCount(2).assertNoErrors();
        assertEquals("https://example.com/base/result.mp4", result.getVideo());
        assertEquals("query", delegate.search);
        assertEquals("genre", delegate.category);
    }

    private VideoHttpResponse<PageInfo<List<VideoRes>>> page(VideoRes video) {
        PageInfo<List<VideoRes>> page = new PageInfo<>(); page.setRecords(Arrays.asList(video));
        VideoHttpResponse<PageInfo<List<VideoRes>>> result = new VideoHttpResponse<>();
        result.setResult(page); return result;
    }

    @Test public void invalidExplicitEpisodesUseTheObservableErrorPath() {
        FakeApi delegate = new FakeApi();
        var result = new SourcedVideoApi(delegate, "https://example.com/api/").getVideoList(1).test();
        VideoRes video = new VideoRes();
        video.setVideo("guess_1.mp4"); video.setEpisodes(3);
        video.setEpisodeUrls(new String[]{"valid.mp4", null});
        delegate.page.onNext(page(video));
        result.assertNoValues().assertError(IllegalArgumentException.class);
    }
    private static class FakeApi implements VideoApis {
        final PublishSubject<VideoHttpResponse<PageInfo<List<VideoRes>>>> page = PublishSubject.create();
        final PublishSubject<VideoHttpResponse<List<VideoRes>>> banner = PublishSubject.create();
        int lastPage;
        String search;
        String category;
        @Override public Observable<VideoHttpResponse<List<VideoRes>>> getVideoBanner() { return banner; }
        @Override public Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getVideoList(int p) { lastPage = p; return page; }
        @Override public Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getSearchVideoList(int p, String q) { lastPage = p; search = q; return page; }
        @Override public Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getTypeVideoList(int p, String q) { lastPage = p; category = q; return page; }
    }
}
